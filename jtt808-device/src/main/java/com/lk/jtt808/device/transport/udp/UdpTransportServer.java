package com.lk.jtt808.device.transport.udp;

import com.lk.jtt808.device.transport.TransportServer;
import com.lk.jtt808.device.transport.TransportType;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * UDP 传输服务器实现
 * 基于 Netty NioDatagramChannel
 */
@Slf4j
public class UdpTransportServer implements TransportServer {

    private final int port;
    private final ChannelInitializer<NioDatagramChannel> channelInitializer;

    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private volatile boolean running = false;

    public UdpTransportServer(int port, ChannelInitializer<NioDatagramChannel> channelInitializer) {
        this.port = port;
        this.channelInitializer = channelInitializer;
    }

    @Override
    public void start() throws Exception {
        workerGroup = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, false)
                .option(ChannelOption.SO_RCVBUF, 65536)
                .option(ChannelOption.SO_SNDBUF, 65536)
                .handler(channelInitializer);

        serverChannel = bootstrap.bind(port).sync().channel();
        running = true;
        log.info("UDP服务器启动成功, 端口: {}", port);
    }

    @Override
    public void stop() {
        running = false;
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("UDP服务器已关闭, 端口: {}", port);
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
        return TransportType.UDP;
    }

    /**
     * 获取 DatagramChannel（供 UdpMessageSender 使用）
     */
    public Channel getServerChannel() {
        return serverChannel;
    }
}
