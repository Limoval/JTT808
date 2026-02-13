package com.lk.jtt808.device.session;


import com.lk.jtt808.device.transport.MessageSender;
import com.lk.jtt808.device.transport.TransportSession;
import com.lk.jtt808.device.transport.TransportType;
import com.lk.jtt808.protocol.entity.JT808Message;
import com.lk.jtt808.protocol.entity.JT808Response;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.IntUnaryOperator;

/**
 * JTT808会话管理类
 * 实现 TransportSession 接口，支持 TCP/UDP 双传输协议
 * 通过 MessageSender 策略模式实现传输层解耦
 */
@Slf4j
@Getter
public class Session implements TransportSession {

    // ==================== 传输相关属性 ====================
    /** 消息发送策略 */
    private final MessageSender messageSender;
    /** 传输协议类型 */
    private final TransportType transportType;
    /** 会话管理器 */
    private final SessionManager sessionManager;
    /** 远程地址字符串（缓存，避免重复toString） */
    private final String remoteAddressStr;

    // ==================== 会话状态属性 ====================
    /** 会话创建时间 */
    private final long creationTime;
    /** 最后访问时间 */
    private long lastAccessedTime;
    /** 会话属性存储（线程安全） */
    private final Map<Object, Object> attributes = new ConcurrentHashMap<>();

    // ==================== 设备标识属性 ====================
    /** 会话ID（通常是 clientId） */
    private String sessionId;
    /** 设备ID（终端手机号） */
    private String clientId;

    // ==================== 消息处理属性 ====================
    /** 消息流水号生成器 */
    private final AtomicInteger serialNoGenerator = new AtomicInteger(0);
    /** 出站消息拦截器（发送前处理） */
    private BiConsumer<TransportSession, JT808Message> outBoundInterceptor = (session, message) -> {
    };

    // ==================== 请求-响应匹配机制 ====================
    /** 等待响应的请求映射 <消息类型或流水号, 响应处理器> */
    private final Map<String, ResponseWaiter> awaitingResponses = new ConcurrentHashMap<>();

    private int defaultTimeoutSeconds = 30;

    private static final ScheduledExecutorService timeoutScheduler =
            Executors.newScheduledThreadPool(
                    Math.max(4, Runtime.getRuntime().availableProcessors()),
                    r -> {
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
            this.timeoutTask = timeoutScheduler.schedule(() -> {
                if (!completed) {
                    completed = true;
                    awaitingMap.remove(responseKey);
                    sink.error(new TimeoutException("请求超时: " + timeout.toSeconds() + "秒"));
                }
            }, timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        public void complete(Object result) {
            if (!completed) {
                completed = true;
                timeoutTask.cancel(false);
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
                   MessageSender messageSender,
                   TransportType transportType) {
        this.sessionManager = sessionManager;
        this.messageSender = messageSender;
        this.transportType = transportType;
        this.remoteAddressStr = messageSender.getRemoteAddress().toString();
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = creationTime;
    }

    // ==================== 会话注册相关方法 ====================

    @Override
    public void register(JT808Message message) {
        register(message.getClientId(), message.getClientId());
    }

    @Override
    public void register(String sessionId, String clientId) {
        if (sessionId == null) {
            throw new NullPointerException("会话ID不能为空");
        }

        this.sessionId = sessionId;
        this.clientId = clientId;

        if (sessionManager != null) {
            sessionManager.add(this);
        }

        log.info("设备注册成功: {}", this);
    }

    @Override
    public boolean isRegistered() {
        return sessionId != null;
    }

    // ==================== 消息发送相关方法 ====================

    @Override
    public Mono<Void> sendNotification(JT808Message message) {
        outBoundInterceptor.accept(this, message);

        return Mono.create(sink -> {
            messageSender.send(message).addListener(future -> {
                if (future.isSuccess()) {
                    sink.success();
                } else {
                    sink.error(future.cause());
                }
            });
        });
    }

    @Override
    public <T> Mono<T> sendRequest(JT808Message request, Class<T> responseClass) {
        return sendRequest(request, responseClass, Duration.ofSeconds(defaultTimeoutSeconds));
    }

    @Override
    public <T> Mono<T> sendRequest(JT808Message request, Class<T> responseClass, Duration timeout) {
        outBoundInterceptor.accept(this, request);

        String responseKey = buildResponseKey(request, responseClass);

        return Mono.create(sink -> {
            ResponseWaiter waiter = new ResponseWaiter(sink, responseKey, awaitingResponses, timeout);

            ResponseWaiter existing = awaitingResponses.putIfAbsent(responseKey, waiter);
            if (existing != null) {
                waiter.error(new RejectedExecutionException("设备暂未响应上一个请求，请勿重复发送"));
                return;
            }

            messageSender.send(request).addListener(future -> {
                if (!future.isSuccess()) {
                    ResponseWaiter removed = awaitingResponses.remove(responseKey);
                    if (removed != null) {
                        removed.error(future.cause());
                    }
                }
            });
        });
    }

    // ==================== 连接信息方法 ====================

    @Override
    public InetSocketAddress getRemoteAddress() {
        return messageSender.getRemoteAddress();
    }

    @Override
    public boolean isActive() {
        return messageSender.isActive();
    }

    @Override
    public void close() {
        messageSender.close();
    }

    // ==================== 响应处理相关方法 ====================

    @Override
    public boolean handleResponse(JT808Response message) {
        String responseKey = buildResponseKey(message);
        ResponseWaiter waiter = awaitingResponses.remove(responseKey);

        if (waiter != null) {
            waiter.complete(message);
            return true;
        }

        return false;
    }

    // ==================== 流水号管理 ====================

    private static final IntUnaryOperator SERIAL_NO_INCREMENTER =
            prev -> prev >= 0xFFFF ? 0 : prev + 1;

    @Override
    public int nextSerialNo() {
        return serialNoGenerator.getAndUpdate(SERIAL_NO_INCREMENTER);
    }

    // ==================== 会话属性管理 ====================

    @Override
    public void setAttribute(Object name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public Object getAttribute(Object name) {
        return attributes.get(name);
    }

    @Override
    public Object removeAttribute(Object name) {
        return attributes.remove(name);
    }

    // ==================== 离线缓存相关 ====================

    public Object getOfflineCache(String clientId) {
        return sessionManager != null ? sessionManager.getOfflineCache(clientId) : null;
    }

    public void setOfflineCache(String clientId, Object value) {
        if (sessionManager != null) {
            sessionManager.setOfflineCache(clientId, value);
        }
    }

    // ==================== 拦截器设置 ====================

    @Override
    public void setOutBoundInterceptor(BiConsumer<TransportSession, JT808Message> outBoundInterceptor) {
        if (outBoundInterceptor != null) {
            this.outBoundInterceptor = outBoundInterceptor;
        }
    }

    // ==================== 会话生命周期管理 ====================

    @Override
    public long updateLastAccessTime() {
        lastAccessedTime = System.currentTimeMillis();
        return lastAccessedTime;
    }

    @Override
    public void invalidate() {
        awaitingResponses.forEach((key, waiter) -> {
            waiter.error(new IllegalStateException("Session closed"));
        });
        awaitingResponses.clear();

        if (isRegistered() && sessionManager != null) {
            sessionManager.remove(this);
        }

        close();

        log.info("session会话已销毁: {}", this);
    }

    // ==================== 私有辅助方法 ====================

    private static String buildResponseKey(JT808Message request, Class responseClass) {
        String className = responseClass.getName();

        if (JT808Response.class.isAssignableFrom(responseClass)) {
            int serialNo = request.getInboundSerialNo();
            return className + "." + serialNo;
        }

        return className;
    }

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
