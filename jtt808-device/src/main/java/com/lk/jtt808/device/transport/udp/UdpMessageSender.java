package com.lk.jtt808.device.transport.udp;

import com.lk.jtt808.device.transport.MessageSender;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;

/**
 * UDP 消息发送实现
 * 通过共享的 DatagramChannel 向指定远程地址发送消息
 */
public class UdpMessageSender implements MessageSender {

    private final Channel datagramChannel;
    private final InetSocketAddress remoteAddress;
    private volatile boolean active = true;

    public UdpMessageSender(Channel datagramChannel, InetSocketAddress remoteAddress) {
        this.datagramChannel = datagramChannel;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public Future<?> send(Object message) {
        UdpOutbound outbound = new UdpOutbound(message, remoteAddress);
        return datagramChannel.writeAndFlush(outbound);
    }

    @Override
    public boolean isActive() {
        return active && datagramChannel.isActive();
    }

    @Override
    public void close() {
        active = false;
        // UDP 不需要关闭共享 Channel，只是标记自身为非活跃
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }
}
