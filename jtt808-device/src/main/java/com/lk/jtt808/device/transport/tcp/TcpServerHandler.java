package com.lk.jtt808.device.transport.tcp;

import com.lk.jtt808.device.session.Session;
import com.lk.jtt808.device.session.SessionManager;
import com.lk.jtt808.device.transport.MessageProcessor;
import com.lk.jtt808.protocol.entity.JT808Message;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * TCP 连接生命周期处理器
 * 管理 TCP 连接的创建、销毁和空闲检测
 * 消息处理委托给 MessageProcessor
 */
@Slf4j
@ChannelHandler.Sharable
public class TcpServerHandler extends SimpleChannelInboundHandler<JT808Message> {

    private static final AttributeKey<Session> SESSION_KEY = AttributeKey.valueOf("session");

    private final SessionManager sessionManager;
    private final MessageProcessor messageProcessor;
    private final int registrationTimeoutSeconds;

    public TcpServerHandler(SessionManager sessionManager,
                            MessageProcessor messageProcessor,
                            int registrationTimeoutSeconds) {
        this.sessionManager = sessionManager;
        this.messageProcessor = messageProcessor;
        this.registrationTimeoutSeconds = registrationTimeoutSeconds;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("新TCP连接: {}", ctx.channel().remoteAddress());
        Session session = sessionManager.createTcpSession(ctx.channel());
        ctx.channel().attr(SESSION_KEY).set(session);
        scheduleConnectionTimeout(ctx, session);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        Session session = getSessionFromChannel(ctx.channel());

        if (session != null) {
            if (session.isRegistered()) {
                log.info("已注册设备断开连接: clientId={}, address={}", session.getClientId(), remoteAddress);
                session.invalidate();
            } else {
                log.info("未注册设备断开连接: address={}", remoteAddress);
            }
        }
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, JT808Message message) {
        Session session = getSessionFromChannel(ctx.channel());
        if (session == null) {
            log.warn("未找到Session，忽略消息: {}", message);
            return;
        }

        messageProcessor.process(message, session);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("TCP连接异常: {}", cause.getMessage());
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event) {
            if (event.state() == IdleState.READER_IDLE) {
                Session session = getSessionFromChannel(ctx.channel());
                String identifier = session != null ? session.getClientId() : ctx.channel().remoteAddress().toString();
                log.warn("TCP连接空闲超时: {}", identifier);
                ctx.close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    private void scheduleConnectionTimeout(ChannelHandlerContext ctx, Session session) {
        ctx.channel().eventLoop().schedule(() -> {
            if (!session.isRegistered()) {
                log.warn("设备连接超时未注册，关闭连接: {}", session.getRemoteAddressStr());
                ctx.channel().close();
            }
        }, registrationTimeoutSeconds, TimeUnit.SECONDS);
    }

    private Session getSessionFromChannel(Channel channel) {
        return channel.attr(SESSION_KEY).get();
    }
}
