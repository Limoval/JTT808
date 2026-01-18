package com.lk.jtt808.device.netty;


import com.lk.jtt808.protocol.codec.Jtt808Encoder;
import com.lk.jtt808.protocol.codec.Jtt808FrameDecoder;
import com.lk.jtt808.protocol.codec.Jtt808MessageDecoder;
import com.lk.jtt808.protocol.codec.Jtt808MessageMapping;
import com.lk.jtt808.device.handler.JTT808ServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final JTT808ServerHandler jtt808ServerHandler;

    /** 读空闲超时时间（秒），超过此时间未收到数据则触发空闲事件 */
    @Value("${jtt808.netty.reader-idle-seconds:180}")
    private int readerIdleSeconds;

    /** 读超时时间（秒），超过此时间未收到数据则关闭连接 */
    @Value("${jtt808.netty.read-timeout-seconds:300}")
    private int readTimeoutSeconds;

    public NettyChannelInitializer(JTT808ServerHandler jtt808ServerHandler) {
        this.jtt808ServerHandler = jtt808ServerHandler;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
            // 空闲检测：超过readerIdleSeconds秒未收到数据则触发IdleStateEvent
            .addLast(new IdleStateHandler(readerIdleSeconds, 0, 0, TimeUnit.SECONDS))
            // 读超时：超过readTimeoutSeconds秒未收到数据则关闭连接
            .addLast(new ReadTimeoutHandler(readTimeoutSeconds, TimeUnit.SECONDS))
            // 帧解码器
            .addLast(new Jtt808FrameDecoder())
            .addLast(new Jtt808MessageDecoder())
            .addLast(new Jtt808MessageMapping())
            .addLast(new Jtt808Encoder())
            // 自定义业务处理器
            .addLast(jtt808ServerHandler);
    }
}
