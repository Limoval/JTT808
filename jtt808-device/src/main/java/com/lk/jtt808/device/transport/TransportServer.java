package com.lk.jtt808.device.transport;

/**
 * 传输服务器接口
 * TCP 和 UDP 服务器的统一抽象
 */
public interface TransportServer {

    /**
     * 启动传输服务器
     */
    void start() throws Exception;

    /**
     * 停止传输服务器
     */
    void stop();

    /**
     * 是否正在运行
     */
    boolean isRunning();

    /**
     * 获取监听端口
     */
    int getPort();

    /**
     * 获取传输类型
     */
    TransportType getTransportType();
}
