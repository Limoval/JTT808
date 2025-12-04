package com.lk.jtt808.netty;

import com.lk.jtt808.protocol.annotation.MessageHandlerRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class NettyServer {
    @Value("${netty.port:8081}")
    private int port;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private final NettyChannelInitializer nettyChannelInitializer;

    public NettyServer(NettyChannelInitializer nettyChannelInitializer) {
        this.nettyChannelInitializer = nettyChannelInitializer;
    }

    @PostConstruct
    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1); // 1个线程处理连接
        workerGroup = new NioEventLoopGroup(); // 默认CPU核心数*2

        try {
            MessageHandlerRegistry.autoRegister("com.lk.jtt808.protocol.entity");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(nettyChannelInitializer) // 自定义初始化器
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        ChannelFuture future = bootstrap.bind(port).sync();
        System.out.println("Netty服务端启动，端口：" + port);
    }

    @PreDestroy
    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        System.out.println("Netty服务端关闭");
    }
}
