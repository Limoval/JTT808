package com.lk.jtt808.protocol.session;


import com.lk.jtt808.protocol.entity.JT808Message;
import com.lk.jtt808.protocol.entity.JT808Response;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

/**
 * JTT808会话管理类
 * 支持连接管理、消息收发、请求-响应模式
 */
@Slf4j
@Getter
public class Session {

    // ==================== 连接相关属性 ====================
    /** Netty通道 */
    private final Channel channel;
    /** 会话管理器 */
    private final SessionManager sessionManager;
    /** 远程地址 */
    private final InetSocketAddress remoteAddress;
    /** 远程地址字符串（缓存，避免重复toString） */
    private final String remoteAddressStr;
    /** 连接关闭回调函数 */
    private final Function<Session, Boolean> connectionCloser;

    // ==================== 会话状态属性 ====================
    /** 会话创建时间 */
    private final long creationTime;
    /** 最后访问时间 */
    private long lastAccessedTime;
    /** 会话属性存储 */
    private final Map<Object, Object> attributes;

    // ==================== 设备标识属性 ====================
    /** 会话ID（通常是 clientId） */
    private String sessionId;
    /** 设备ID（终端手机号） */
    private String clientId;

    // ==================== 消息处理属性 ====================
    /** 消息流水号生成器 */
    private final AtomicInteger serialNoGenerator = new AtomicInteger(0);
    /** 出站消息拦截器（发送前处理） */
    private BiConsumer<Session, JT808Message> outBoundInterceptor = (session, message) -> {

    };

    // ==================== 请求-响应匹配机制 ====================
    /** 等待响应的请求映射 <消息类型或流水号, 响应处理器> */
    private final Map<String, ResponseWaiter> awaitingResponses = new ConcurrentHashMap<>();

    @Value("${jtt808.request.timeout:30}")
    private int defaultTimeoutSeconds = 30;

    /** 请求被拒绝时返回的错误 */
    private static final Mono REJECTED_REQUEST = Mono.error(
            new RejectedExecutionException("设备暂未响应上一个请求，请勿重复发送")
    );

    private static final ScheduledExecutorService timeoutScheduler =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "jtt808-timeout-cleaner");
                t.setDaemon(true);
                return t;
            });

    private static class ResponseWaiter {
        private final MonoSink sink;
        private final ScheduledFuture<?> timeoutTask;
        private volatile boolean completed = false;

        public ResponseWaiter(MonoSink sink, String responseKey,
                              Map<String, ResponseWaiter> awaitingMap,
                              Duration timeout) {
            this.sink = sink;

            // 创建超时清理任务
            this.timeoutTask = timeoutScheduler.schedule(() -> {
                if (!completed) {
                    completed = true;
                    awaitingMap.remove(responseKey); // 从等待队列移除
                    sink.error(new TimeoutException("请求超时: " + timeout.toSeconds() + "秒"));
                }
            }, timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        public void complete(Object result) {
            if (!completed) {
                completed = true;
                timeoutTask.cancel(false); // 取消超时任务
                sink.success(result);
            }
        }

        public void error(Throwable throwable) {
            if (!completed) {
                completed = true;
                timeoutTask.cancel(false);
                sink.error(throwable);
            }
        }
    }

    public Session(SessionManager sessionManager,
                   Channel channel,
                   InetSocketAddress remoteAddress,
                   Function<Session, Boolean> connectionCloser) {
        // 初始化连接信息
        this.sessionManager = sessionManager;
        this.channel = channel;
        this.remoteAddress = remoteAddress;
        this.remoteAddressStr = remoteAddress.toString();
        this.connectionCloser = connectionCloser;

        // 初始化时间戳
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = creationTime;

        // 初始化属性存储
        this.attributes = new TreeMap<>();
    }

    // ==================== 会话注册相关方法 ====================

    /**
     * 注册会话到SessionManager
     * 通常在终端鉴权通过后调用
     *
     * @param message 注册消息（包含clientId）
     */
    public void register(JT808Message message) {
        register(message.getClientId(), message.getClientId());
    }

    /**
     * 注册会话到SessionManager
     *
     * @param sessionId 会话ID（通常使用clientId）
     * @param clientId clientId
     */
    public void register(String sessionId, String clientId) {
        if (sessionId == null) {
            throw new NullPointerException("会话ID不能为空");
        }

        this.sessionId = sessionId;
        this.clientId = clientId;

        // 添加到SessionManager
        if (sessionManager != null) {
            sessionManager.add(this);
        }

        log.info("设备注册成功: {}", this);
    }

    /**
     * 检查会话是否已注册
     */
    public boolean isRegistered() {
        return sessionId != null;
    }

    // ==================== 消息发送相关方法 ====================

    /**
     * 发送通知消息（不需要等待响应）
     * 适用于: 平台通知、实时指令等
     *
     * @param message 要发送的消息
     * @return 发送结果的异步对象
     */
    public Mono<Void> sendNotification(JT808Message message) {
        // 执行请求拦截器（自动设置clientId、流水号等）
        outBoundInterceptor.accept(this, message);

        return Mono.create(sink -> {
            channel.writeAndFlush(message).addListener(future -> {
                if (future.isSuccess()) {
                    sink.success();
                } else {
                    sink.error(future.cause());
                }
            });
        });
    }

    /**
     * 发送请求消息并等待指定类型的响应
     * 适用于: 参数设置、位置查询等需要确认的命令
     *
     * @param request 请求消息
     * @param responseClass 期望的响应消息类型
     * @return 响应消息的异步对象
     */
    public <T> Mono<T> sendRequest(JT808Message request, Class<T> responseClass) {
        return sendRequest(request, responseClass, Duration.ofSeconds(defaultTimeoutSeconds));
    }

    public <T> Mono<T> sendRequest(JT808Message request, Class<T> responseClass, Duration timeout) {
        // 执行请求拦截器
        outBoundInterceptor.accept(this, request);

        // 生成响应匹配的key
        String responseKey = buildResponseKey(request, responseClass);

        return Mono.create(sink -> {
            //创建带超时的等待器
            ResponseWaiter waiter = new ResponseWaiter(sink, responseKey, awaitingResponses, timeout);

            //检查重复请求
            ResponseWaiter existing = awaitingResponses.putIfAbsent(responseKey, waiter);
            if (existing != null) {
                waiter.error(new RejectedExecutionException("设备暂未响应上一个请求，请勿重复发送"));
                return;
            }

            //发送请求
            channel.writeAndFlush(request).addListener(future -> {
                if (!future.isSuccess()) {
                    //发送失败，清理等待器
                    ResponseWaiter removed = awaitingResponses.remove(responseKey);
                    if (removed != null) {
                        removed.error(future.cause());
                    }
                }
            });
        });
    }

    // ==================== 响应处理相关方法 ====================

    /**
     * 处理收到的响应消息
     * 在收到终端响应时调用，用于完成对应的请求等待
     *
     * @param message 响应消息
     * @return 是否成功匹配到等待的请求
     */
    public boolean handleResponse(JT808Response message) {
        // 执行响应拦截器
        //responseInterceptor.accept(this, message);

        // 构建响应key并查找等待的请求
        String responseKey = buildResponseKey(message);
        ResponseWaiter waiter = awaitingResponses.remove(responseKey); // 直接移除

        if (waiter != null) {
            waiter.complete(message);
            return true;
        }

        return false; // 没有找到对应的等待请求
    }

    // ==================== 流水号管理 ====================

    /** 流水号递增操作（0xFFFF后归零） */
    private static final IntUnaryOperator SERIAL_NO_INCREMENTER =
            prev -> prev >= 0xFFFF ? 0 : prev + 1;

    /**
     * 获取下一个消息流水号
     * JTT808协议要求流水号范围: 0-65535
     */
    public int nextSerialNo() {
        return serialNoGenerator.getAndUpdate(SERIAL_NO_INCREMENTER);
    }

    // ==================== 会话属性管理 ====================

    /**
     * 设置会话属性
     * 可用于存储设备信息、状态数据等
     */
    public void setAttribute(Object name, Object value) {
        attributes.put(name, value);
    }

    public Object getAttribute(Object name) {
        return attributes.get(name);
    }

    public Object removeAttribute(Object name) {
        return attributes.remove(name);
    }

    // ==================== 离线缓存相关 ====================

    /**
     * 获取设备的离线缓存数据
     */
    public Object getOfflineCache(String clientId) {
        return sessionManager != null ? sessionManager.getOfflineCache(clientId) : null;
    }

    /**
     * 设置设备的离线缓存数据
     */
    public void setOfflineCache(String clientId, Object value) {
        if (sessionManager != null) {
            sessionManager.setOfflineCache(clientId, value);
        }
    }

    // ==================== 拦截器设置 ====================

    /**
     * 设置请求拦截器
     * 用于在发送消息前自动处理消息属性
     */
    public void setOutBoundInterceptor(BiConsumer<Session, JT808Message> outBoundInterceptor) {
        if (outBoundInterceptor != null) {
            this.outBoundInterceptor = outBoundInterceptor;
        }
    }



    // ==================== 会话生命周期管理 ====================

    /**
     * 更新最后访问时间
     * 用于会话超时检测
     */
    public long updateLastAccessTime() {
        lastAccessedTime = System.currentTimeMillis();
        return lastAccessedTime;
    }

    /**
     * 销毁会话
     * 清理资源并关闭连接
     */
    public void invalidate() {
        // 从SessionManager中移除
        if (isRegistered() && sessionManager != null) {
            sessionManager.remove(this);
        }

        // 关闭连接
        connectionCloser.apply(this);

        log.info("session会话已销毁: {}", this);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建请求的响应匹配key
     */
    private static String buildResponseKey(JT808Message request, Class responseClass) {
        String className = responseClass.getName();

        //如果是通用应答类型，需要加上流水号进行精确匹配
        if (JT808Response.class.isAssignableFrom(responseClass)) {
            int serialNo = request.getInboundSerialNo();
            return className + "." + serialNo;
        }

        //其他类型直接用类名匹配
        return className;
    }

    /**
     * 构建响应消息的匹配key
     */
    private static String buildResponseKey(Object response) {
        String className = response.getClass().getName();

        if (response instanceof JT808Response) {
            int serialNo = ((JT808Response) response).getResponseSerialNo();
            return className + "." + serialNo;
        }

        return className;
    }

    public static void shutdownTimeoutScheduler() {
        if (!timeoutScheduler.isShutdown()) {
            timeoutScheduler.shutdown();
            try {
                if (!timeoutScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    timeoutScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                timeoutScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
