package com.lk.jtt808.device.transport.tcp;

import com.lk.jtt808.device.transport.MessageSender;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;

/**
 * TCP 消息发送实现
 * 基于 Netty Channel 的 writeAndFlush
 */
public class TcpMessageSender implements MessageSender {

    private final Channel channel;

    public TcpMessageSender(Channel channel) {
        this.channel = channel;
    }

    @Override
    public Future<?> send(Object message) {
        return channel.writeAndFlush(message);
    }

    @Override
    public boolean isActive() {
        return channel.isActive();
    }

    @Override
    public void close() {
        if (channel.isActive()) {
            channel.close();
        }
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) channel.remoteAddress();
    }

    /**
     * 获取底层 Channel（仅在需要底层操作时使用）
     */
    public Channel getChannel() {
        return channel;
    }
}
