package com.lk.jtt808.device.transport;

import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;

/**
 * 消息发送策略接口
 * TCP 和 UDP 各有不同的发送实现
 */
public interface MessageSender {

    /**
     * 发送消息
     *
     * @param message 待发送消息
     * @return 发送结果 Future
     */
    Future<?> send(Object message);

    /**
     * 连接是否活跃
     */
    boolean isActive();

    /**
     * 关闭连接
     */
    void close();

    /**
     * 获取远程地址
     */
    InetSocketAddress getRemoteAddress();
}
