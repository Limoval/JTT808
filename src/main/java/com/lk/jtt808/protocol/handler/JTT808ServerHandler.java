package com.lk.jtt808.protocol.handler;


import com.lk.jtt808.protocol.entity.JT808Message;
import com.lk.jtt808.protocol.entity.JT808Response;
import com.lk.jtt808.protocol.session.Session;
import com.lk.jtt808.protocol.session.SessionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ChannelHandler.Sharable
public class JTT808ServerHandler extends SimpleChannelInboundHandler<JT808Message> {

    private final SessionManager sessionManager;

    private final MessageHandlerDispatcher messageHandlerDispatcher;

    public JTT808ServerHandler(SessionManager sessionManager, MessageHandlerDispatcher messageHandlerDispatcher) {
        this.sessionManager = sessionManager;
        this.messageHandlerDispatcher = messageHandlerDispatcher;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("新连接: {}", ctx.channel().remoteAddress());
        Session session = sessionManager.createTcpSession(ctx.channel());
        ctx.channel().attr(AttributeKey.valueOf("session")).set(session);
        //必须注册,否则30秒后断开连接
        scheduleConnectionTimeout(ctx, session);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        //获取channelActive中添加的Session
        Session session = getSessionFromChannel(ctx.channel());

        if (session != null) {
            //如果设备已注册，则进行清理
            if (session.isRegistered()) {
                log.info("已注册设备断开连接: clientId={}, address={}", session.getClientId(), remoteAddress);
                //销毁Session（会自动从SessionManager中移除）
                session.invalidate();
            } else {
                log.info("未注册设备断开连接: address={}", remoteAddress);
            }
        }
        super.channelInactive(ctx);

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, JT808Message message) throws Exception {

        Session session = getSessionFromChannel(ctx.channel());
        if (session == null) {
            log.warn("未找到Session，忽略消息: {}", message);
            return;
        }
        session.updateLastAccessTime();

        //如果属于响应消息
        if (message instanceof JT808Response) {
            if (session.handleResponse((JT808Response) message)) {
                return;
            }
        }

        //向下分发消息
        messageHandlerDispatcher.dispatch(message, session);

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("发生异常: {}", cause.getMessage());
        ctx.close();
    }

    /**
     * 设置连接超时检查
     */
    private void scheduleConnectionTimeout(ChannelHandlerContext ctx, Session session) {
        // 30秒内必须收到注册消息，否则关闭连接
        ctx.channel().eventLoop().schedule(() -> {
            if (!session.isRegistered()) {
                log.warn("设备连接超时未注册，关闭连接: {}", session.getRemoteAddressStr());
                ctx.channel().close();
            }
        }, 30, TimeUnit.SECONDS);
    }

    /**
     * 从Channel获取Session
     */
    private Session getSessionFromChannel(Channel channel) {
        return (Session) channel.attr(AttributeKey.valueOf("session")).get();
    }
}