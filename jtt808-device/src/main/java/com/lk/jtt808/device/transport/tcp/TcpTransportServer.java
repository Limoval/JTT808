package com.lk.jtt808.device.transport.tcp;

import com.lk.jtt808.device.transport.TransportServer;
import com.lk.jtt808.device.transport.TransportType;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * TCP 传输服务器实现
 * 基于 Netty NioServerSocketChannel
 */
@Slf4j
public class TcpTransportServer implements TransportServer {

    private final int port;
    private final ChannelInitializer<SocketChannel> channelInitializer;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private volatile boolean running = false;

    public TcpTransportServer(int port, ChannelInitializer<SocketChannel> channelInitializer) {
        this.port = port;
        this.channelInitializer = channelInitializer;
    }

    @Override
    public void start() throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(channelInitializer)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);

        serverChannel = bootstrap.bind(port).sync().channel();
        running = true;
        log.info("TCP服务器启动成功, 端口: {}", port);
    }

    @Override
    public void stop() {
        running = false;
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("TCP服务器已关闭, 端口: {}", port);
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public TransportType getTransportType() {
        return TransportType.TCP;
    }
}
