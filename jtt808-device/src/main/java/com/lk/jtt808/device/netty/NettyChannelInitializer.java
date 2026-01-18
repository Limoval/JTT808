package com.lk.jtt808.device.netty;


import com.lk.jtt808.protocol.codec.Jtt808Encoder;
import com.lk.jtt808.protocol.codec.Jtt808FrameDecoder;
import com.lk.jtt808.protocol.codec.Jtt808MessageDecoder;
import com.lk.jtt808.protocol.codec.Jtt808MessageMapping;
import com.lk.jtt808.device.handler.JTT808ServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.springframework.stereotype.Component;


@Component
public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final JTT808ServerHandler jtt808ServerHandler;

    public NettyChannelInitializer(JTT808ServerHandler jtt808ServerHandler) {
        this.jtt808ServerHandler = jtt808ServerHandler;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
            .addLast(new Jtt808FrameDecoder())
            .addLast(new Jtt808MessageDecoder())
            .addLast(new Jtt808MessageMapping())
            .addLast(new Jtt808Encoder())
            // 自定义业务处理器
            .addLast(jtt808ServerHandler);
    }
}
