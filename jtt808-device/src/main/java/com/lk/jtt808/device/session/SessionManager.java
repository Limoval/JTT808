package com.lk.jtt808.device.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.channel.Channel;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SessionManager {

    // ==================== 核心存储 ====================
    /** 会话存储 <sessionId, Session> */
    private final ConcurrentHashMap<String, Session> sessionRegistry;

    /** 离线数据缓存 <clientId, 离线数据> */
    private final Cache<String, Object> offlineDataCache;

    // ==================== 配置和监听 ====================
    /** 会话生命周期监听器 */
    private final SessionListener sessionListener;

    /**
     * 构造方法
     *
     * @param sessionListener 会话生命周期监听器
     */
    public SessionManager(SessionListener sessionListener) {
        this.sessionRegistry = new ConcurrentHashMap<>();
        this.sessionListener = sessionListener;

        // 配置离线缓存：10分钟无访问后过期
        this.offlineDataCache = Caffeine.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .maximumSize(10000) // 最大缓存1万个设备的离线数据
                .build();
    }

    // ==================== 会话创建和销毁 ====================

    public Session createTcpSession(Channel channel) {
        InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();

        Session session = new Session(
                this,
                channel,
                remoteAddress,
                s -> closeConnection(channel)
        );

        // 触发会话创建事件
        notifySessionCreated(session);

        log.info("TCP会话已创建: {}", session);
        return session;
    }

    @PreDestroy
    public void shutdown() {
        // 关闭超时调度器
        Session.shutdownTimeoutScheduler();

        // 关闭所有会话
        sessionRegistry.values().forEach(Session::invalidate);
    }


    // ==================== 会话注册和查询 ====================

    /**
     * 注册会话到管理器
     * 通常在设备鉴权通过后调用
     *
     * @param session 要注册的会话
     */
    protected void add(Session session) {
        // 检查是否有旧会话需要替换
        Session oldSession = sessionRegistry.put(session.getSessionId(), session);

        if (oldSession != null && !oldSession.equals(session)) {
            log.warn("替换已存在的会话: {} -> {}", oldSession, session);
            oldSession.invalidate(); // 关闭旧会话
        }

        // 触发会话注册事件
        notifySessionRegistered(session);

        log.info("会话已注册: {}", session);
    }

    /**
     * 从管理器中移除会话
     *
     * @param session 要移除的会话
     */
    protected void remove(Session session) {
        boolean removed = sessionRegistry.remove(session.getSessionId(), session);

        if (removed) {
            // 触发会话销毁事件
            notifySessionDestroyed(session);
            log.info("会话已移除: {}", session);
        }
    }

    /**
     * 根据会话ID获取会话
     *
     * @param sessionId 会话ID（通常是设备ID）
     * @return 会话对象，不存在则返回null
     */
    public Session getSession(String sessionId) {
        return sessionRegistry.get(sessionId);
    }

    /**
     * 获取所有会话
     *
     * @return 所有会话的集合
     */
    public Collection<Session> getAllSessions() {
        return sessionRegistry.values();
    }

    /**
     * 获取所有在线会话
     *
     * @return 在线会话的集合
     */
    public Collection<Session> getOnlineSessions() {
        return sessionRegistry.values().stream()
                .filter(Session::isRegistered)
                .collect(Collectors.toList());
    }

    /**
     * 检查设备是否在线
     *
     * @param clientId 设备ID
     * @return 是否在线
     */
    public boolean isDeviceOnline(String clientId) {
        Session session = sessionRegistry.get(clientId);
        return session != null && session.isRegistered();
    }

    // ==================== 离线缓存管理 ====================

    /**
     * 设置设备的离线缓存数据
     * 用于存储设备离线期间的重要数据
     *
     * @param clientId 设备ID
     * @param data 要缓存的数据
     */
    public void setOfflineCache(String clientId, Object data) {
        offlineDataCache.put(clientId, data);
        log.debug("设置离线缓存: clientId={}", clientId);
    }

    /**
     * 获取设备的离线缓存数据
     *
     * @param clientId 设备ID
     * @return 缓存的数据，不存在则返回null
     */
    public Object getOfflineCache(String clientId) {
        return offlineDataCache.getIfPresent(clientId);
    }

    /**
     * 清除设备的离线缓存
     *
     * @param clientId 设备ID
     */
    public void clearOfflineCache(String clientId) {
        offlineDataCache.invalidate(clientId);
        log.debug("清除离线缓存: clientId={}", clientId);
    }

    // ==================== 会话生命周期事件通知 ====================

    /**
     * 通知会话创建事件
     */
    private void notifySessionCreated(Session session) {
        if (sessionListener != null) {
            try {
                sessionListener.sessionCreated(session);
            } catch (Exception e) {
                log.error("会话创建事件处理异常: {}", session, e);
            }
        }
    }

    /**
     * 通知会话注册事件
     */
    private void notifySessionRegistered(Session session) {
        if (sessionListener != null) {
            try {
                sessionListener.sessionRegistered(session);
            } catch (Exception e) {
                log.error("会话注册事件处理异常: {}", session, e);
            }
        }
    }

    /**
     * 通知会话销毁事件
     */
    private void notifySessionDestroyed(Session session) {
        if (sessionListener != null) {
            try {
                sessionListener.sessionDestroyed(session);
            } catch (Exception e) {
                log.error("会话销毁事件处理异常: {}", session, e);
            }
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 关闭网络连接
     */
    private Boolean closeConnection(Channel channel) {
        if (channel != null && channel.isActive()) {
            channel.close();
            return true;
        }
        return false;
    }

}
