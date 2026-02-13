package com.lk.jtt808.device.transport.udp;

import com.lk.jtt808.device.session.Session;
import com.lk.jtt808.device.session.SessionManager;
import com.lk.jtt808.device.transport.MessageProcessor;
import com.lk.jtt808.device.transport.TransportType;
import com.lk.jtt808.protocol.entity.JT808Message;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * UDP 消息处理器
 * 首包创建 Session，心跳超时清理不活跃 Session
 */
@Slf4j
@ChannelHandler.Sharable
public class UdpServerHandler extends SimpleChannelInboundHandler<JT808Message> {

    private static final AttributeKey<InetSocketAddress> SENDER_KEY = AttributeKey.valueOf("udp_sender");

    private final SessionManager sessionManager;
    private final MessageProcessor messageProcessor;

    /** UDP Session 映射 <远程地址字符串, Session> */
    private final Map<String, Session> udpSessions = new ConcurrentHashMap<>();

    /** 不活跃 Session 清理调度器 */
    private final ScheduledExecutorService cleanupScheduler;

    /** Session 超时时间（秒） */
    private final int sessionTimeoutSeconds;

    public UdpServerHandler(SessionManager sessionManager,
                            MessageProcessor messageProcessor,
                            int sessionTimeoutSeconds) {
        this.sessionManager = sessionManager;
        this.messageProcessor = messageProcessor;
        this.sessionTimeoutSeconds = sessionTimeoutSeconds;

        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "udp-session-cleaner");
            t.setDaemon(true);
            return t;
        });

        // 定期清理不活跃的 UDP Session
        this.cleanupScheduler.scheduleAtFixedRate(
                this::cleanupInactiveSessions, 60, 60, TimeUnit.SECONDS);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, JT808Message message) {
        // 从消息属性获取发送者地址（由 UdpFrameDecoder 设置）
        InetSocketAddress sender = ctx.channel().attr(SENDER_KEY).get();
        if (sender == null) {
            log.warn("UDP消息缺少发送者地址，忽略");
            return;
        }

        String addressKey = sender.toString();
        Session session = udpSessions.get(addressKey);

        // 首包创建 Session
        if (session == null) {
            log.info("新UDP连接: {}", sender);
            UdpMessageSender udpSender = new UdpMessageSender(ctx.channel(), sender);
            session = sessionManager.createSession(udpSender, TransportType.UDP);
            udpSessions.put(addressKey, session);
        }

        messageProcessor.process(message, session);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("UDP处理异常: {}", cause.getMessage());
    }

    /**
     * 设置发送者地址到 Channel Attribute
     * 由 UdpFrameDecoder 调用
     */
    public static void setSenderAddress(Channel channel, InetSocketAddress sender) {
        channel.attr(SENDER_KEY).set(sender);
    }

    /**
     * 清理不活跃的 UDP Session
     */
    private void cleanupInactiveSessions() {
        long now = System.currentTimeMillis();
        long timeoutMillis = sessionTimeoutSeconds * 1000L;

        udpSessions.entrySet().removeIf(entry -> {
            Session session = entry.getValue();
            if (!session.isActive() || (now - session.getLastAccessedTime()) > timeoutMillis) {
                log.info("清理不活跃UDP Session: {}", entry.getKey());
                session.invalidate();
                return true;
            }
            return false;
        });
    }

    /**
     * 关闭清理调度器
     */
    public void shutdown() {
        cleanupScheduler.shutdown();
        udpSessions.values().forEach(Session::invalidate);
        udpSessions.clear();
    }
}
