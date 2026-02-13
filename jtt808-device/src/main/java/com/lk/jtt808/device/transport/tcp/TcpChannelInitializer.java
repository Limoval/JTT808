package com.lk.jtt808.device.transport.tcp;

import com.lk.jtt808.protocol.codec.Jtt808Encoder;
import com.lk.jtt808.protocol.codec.Jtt808FrameDecoder;
import com.lk.jtt808.protocol.codec.Jtt808MessageDecoder;
import com.lk.jtt808.protocol.codec.Jtt808MessageMapping;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.concurrent.TimeUnit;

/**
 * TCP Channel 初始化器
 * 配置 TCP 连接的 Netty Pipeline
 */
public class TcpChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final TcpServerHandler tcpServerHandler;
    private final int readerIdleSeconds;
    private final int readTimeoutSeconds;

    public TcpChannelInitializer(TcpServerHandler tcpServerHandler,
                                 int readerIdleSeconds,
                                 int readTimeoutSeconds) {
        this.tcpServerHandler = tcpServerHandler;
        this.readerIdleSeconds = readerIdleSeconds;
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
                .addLast(new IdleStateHandler(readerIdleSeconds, 0, 0, TimeUnit.SECONDS))
                .addLast(new ReadTimeoutHandler(readTimeoutSeconds, TimeUnit.SECONDS))
                .addLast(new Jtt808FrameDecoder())
                .addLast(new Jtt808MessageDecoder())
                .addLast(new Jtt808MessageMapping())
                .addLast(new Jtt808Encoder())
                .addLast(tcpServerHandler);
    }
}
