package com.lk.jtt808.device.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lk.jtt808.device.transport.MessageSender;
import com.lk.jtt808.device.transport.TransportType;
import com.lk.jtt808.device.transport.tcp.TcpMessageSender;
import io.netty.channel.Channel;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    /** 心跳超时时间（毫秒） */
    private final long heartbeatTimeoutMillis;

    // ==================== 统计指标 ====================
    private final java.util.concurrent.atomic.AtomicLong totalConnections = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong totalDisconnections = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong authFailures = new java.util.concurrent.atomic.AtomicLong(0);

    public SessionManager(SessionListener sessionListener,
                          @Value("${jtt808.heartbeat-interval:60}") long heartbeatIntervalSeconds) {
        this.sessionRegistry = new ConcurrentHashMap<>();
        this.sessionListener = sessionListener;
        this.heartbeatTimeoutMillis = (heartbeatIntervalSeconds * 2 + 10) * 1000L;

        this.offlineDataCache = Caffeine.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();
    }

    // ==================== 会话创建 ====================

    /**
     * 通用会话创建方法
     *
     * @param messageSender 消息发送策略
     * @param transportType 传输类型
     * @return 新创建的会话
     */
    public Session createSession(MessageSender messageSender, TransportType transportType) {
        Session session = new Session(this, messageSender, transportType);

        totalConnections.incrementAndGet();
        notifySessionCreated(session);

        log.info("{}会话已创建: remote={}", transportType, session.getRemoteAddressStr());
        return session;
    }

    /**
     * TCP 会话创建便捷方法
     *
     * @param channel Netty Channel
     * @return 新创建的 TCP 会话
     */
    public Session createTcpSession(Channel channel) {
        MessageSender sender = new TcpMessageSender(channel);
        return createSession(sender, TransportType.TCP);
    }

    @PreDestroy
    public void shutdown() {
        Session.shutdownTimeoutScheduler();
        sessionRegistry.values().forEach(Session::invalidate);
    }

    // ==================== 会话注册和查询 ====================

    protected void add(Session session) {
        Session oldSession = sessionRegistry.put(session.getSessionId(), session);

        if (oldSession != null && !oldSession.equals(session)) {
            log.warn("替换已存在的会话: {} -> {}", oldSession, session);
            oldSession.invalidate();
        }

        notifySessionRegistered(session);
        log.info("会话已注册: {}", session);
    }

    protected void remove(Session session) {
        boolean removed = sessionRegistry.remove(session.getSessionId(), session);

        if (removed) {
            totalDisconnections.incrementAndGet();
            notifySessionDestroyed(session);
            log.info("会话已移除: {}", session);
        }
    }

    public Session getSession(String sessionId) {
        return sessionRegistry.get(sessionId);
    }

    public Collection<Session> getAllSessions() {
        return sessionRegistry.values();
    }

    public Collection<Session> getOnlineSessions() {
        return sessionRegistry.values().stream()
                .filter(Session::isRegistered)
                .filter(Session::isAuthenticated)
                .collect(Collectors.toList());
    }

    public boolean isDeviceOnline(String clientId) {
        Session session = sessionRegistry.get(clientId);
        return session != null && session.isRegistered() && session.isAuthenticated();
    }

    // ==================== 心跳超时会话清理 ====================

    /**
     * 定时清理超时会话（每60秒执行一次）
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupTimeoutSessions() {
        long now = System.currentTimeMillis();
        int cleaned = 0;

        for (Session session : sessionRegistry.values()) {
            if (session.isRegistered() && session.isAuthenticated()) {
                long lastAccessed = session.getLastAccessedTime();
                if ((now - lastAccessed) > heartbeatTimeoutMillis) {
                    log.warn("会话心跳超时，强制清理: clientId={}, idle={}ms",
                            session.getClientId(), (now - lastAccessed));
                    session.invalidate();
                    cleaned++;
                }
            }
        }

        if (cleaned > 0) {
            log.info("心跳超时会话清理完成: 清理 {} 个会话", cleaned);
        }
    }

    // ==================== 离线缓存管理 ====================

    public void setOfflineCache(String clientId, Object data) {
        offlineDataCache.put(clientId, data);
        log.debug("设置离线缓存: clientId={}", clientId);
    }

    public Object getOfflineCache(String clientId) {
        return offlineDataCache.getIfPresent(clientId);
    }

    public void clearOfflineCache(String clientId) {
        offlineDataCache.invalidate(clientId);
        log.debug("清除离线缓存: clientId={}", clientId);
    }

    // ==================== 会话生命周期事件通知 ====================

    private void notifySessionCreated(Session session) {
        if (sessionListener != null) {
            try {
                sessionListener.sessionCreated(session);
            } catch (Exception e) {
                log.error("会话创建事件处理异常: {}", session, e);
            }
        }
    }

    private void notifySessionRegistered(Session session) {
        if (sessionListener != null) {
            try {
                sessionListener.sessionRegistered(session);
            } catch (Exception e) {
                log.error("会话注册事件处理异常: {}", session, e);
            }
        }
    }

    private void notifySessionDestroyed(Session session) {
        if (sessionListener != null) {
            try {
                sessionListener.sessionDestroyed(session);
            } catch (Exception e) {
                log.error("会话销毁事件处理异常: {}", session, e);
            }
        }
    }

    // ==================== 统计指标方法 ====================

    public void incrementAuthFailure() {
        authFailures.incrementAndGet();
    }

    public int getOnlineCount() {
        return (int) sessionRegistry.values().stream()
                .filter(Session::isRegistered)
                .filter(Session::isAuthenticated)
                .count();
    }

    public int getConnectionCount() {
        return sessionRegistry.size();
    }

    public long getTotalConnections() {
        return totalConnections.get();
    }

    public long getTotalDisconnections() {
        return totalDisconnections.get();
    }

    public long getAuthFailures() {
        return authFailures.get();
    }

    public java.util.Map<String, Object> getMetrics() {
        java.util.Map<String, Object> metrics = new java.util.LinkedHashMap<>();
        metrics.put("onlineCount", getOnlineCount());
        metrics.put("connectionCount", getConnectionCount());
        metrics.put("totalConnections", getTotalConnections());
        metrics.put("totalDisconnections", getTotalDisconnections());
        metrics.put("authFailures", getAuthFailures());
        metrics.put("offlineCacheSize", offlineDataCache.estimatedSize());
        return metrics;
    }
}
