package com.lk.jtt808.device.netty;

import com.lk.jtt808.device.handler.JTT808ServerHandler;
import com.lk.jtt808.device.session.SessionManager;
import com.lk.jtt808.device.transport.MessageProcessor;
import com.lk.jtt808.device.transport.TransportServer;
import com.lk.jtt808.device.transport.tcp.TcpChannelInitializer;
import com.lk.jtt808.device.transport.tcp.TcpServerHandler;
import com.lk.jtt808.device.transport.tcp.TcpTransportServer;
import com.lk.jtt808.protocol.annotation.MessageHandlerRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Netty 服务引导类
 * 根据配置启动 TCP 和/或 UDP 传输服务器
 */
@Component
@Slf4j
public class NettyServer {

    @Value("${jtt808.transport.tcp.port:8082}")
    private int tcpPort;

    @Value("${jtt808.transport.tcp.enabled:true}")
    private boolean tcpEnabled;

    @Value("${jtt808.transport.tcp.reader-idle-seconds:180}")
    private int readerIdleSeconds;

    @Value("${jtt808.transport.tcp.read-timeout-seconds:300}")
    private int readTimeoutSeconds;

    @Value("${jtt808.registration.timeout-seconds:30}")
    private int registrationTimeoutSeconds;

    private final SessionManager sessionManager;
    private final MessageProcessor messageProcessor;
    private final NettyChannelInitializer legacyChannelInitializer;
    private final JTT808ServerHandler legacyServerHandler;

    private final List<TransportServer> transportServers = new ArrayList<>();

    public NettyServer(SessionManager sessionManager,
                       MessageProcessor messageProcessor,
                       NettyChannelInitializer legacyChannelInitializer,
                       JTT808ServerHandler legacyServerHandler) {
        this.sessionManager = sessionManager;
        this.messageProcessor = messageProcessor;
        this.legacyChannelInitializer = legacyChannelInitializer;
        this.legacyServerHandler = legacyServerHandler;
    }

    @PostConstruct
    public void start() throws Exception {
        // 注册协议消息类型
        try {
            MessageHandlerRegistry.autoRegister("com.lk.jtt808.protocol.entity");
        } catch (IOException e) {
            throw new RuntimeException("协议消息注册失败", e);
        }

        // 启动 TCP 服务器
        if (tcpEnabled) {
            startTcpServer();
        }

        log.info("JTT808服务启动完成");
    }

    private void startTcpServer() throws Exception {
        TcpServerHandler tcpServerHandler = new TcpServerHandler(
                sessionManager, messageProcessor, registrationTimeoutSeconds);
        TcpChannelInitializer tcpChannelInitializer = new TcpChannelInitializer(
                tcpServerHandler, readerIdleSeconds, readTimeoutSeconds);

        TcpTransportServer tcpServer = new TcpTransportServer(tcpPort, tcpChannelInitializer);
        tcpServer.start();
        transportServers.add(tcpServer);
    }

    @PreDestroy
    public void stop() {
        transportServers.forEach(TransportServer::stop);
        transportServers.clear();
        log.info("JTT808服务已关闭");
    }
}
