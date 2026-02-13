package com.lk.jtt808.device.transport;

import com.lk.jtt808.protocol.entity.JT808Message;
import com.lk.jtt808.protocol.entity.JT808Response;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.function.BiConsumer;

/**
 * 传输会话接口
 * 所有 Handler 通过此接口与设备通信，不依赖具体传输实现
 */
public interface TransportSession {

    // ==================== 消息发送 ====================

    /**
     * 发送通知消息（无需等待响应）
     */
    Mono<Void> sendNotification(JT808Message message);

    /**
     * 发送请求消息（等待响应，使用默认超时）
     */
    <T> Mono<T> sendRequest(JT808Message request, Class<T> responseClass);

    /**
     * 发送请求消息（等待响应，指定超时时间）
     */
    <T> Mono<T> sendRequest(JT808Message request, Class<T> responseClass, Duration timeout);

    // ==================== 连接信息 ====================

    /**
     * 获取远程地址
     */
    InetSocketAddress getRemoteAddress();

    /**
     * 获取远程地址字符串
     */
    String getRemoteAddressStr();

    /**
     * 连接是否活跃
     */
    boolean isActive();

    /**
     * 关闭连接
     */
    void close();

    // ==================== 会话标识 ====================

    /**
     * 获取会话ID
     */
    String getSessionId();

    /**
     * 获取设备ID（终端手机号）
     */
    String getClientId();

    /**
     * 是否已注册
     */
    boolean isRegistered();

    /**
     * 使用消息注册设备
     */
    void register(JT808Message message);

    /**
     * 使用指定 sessionId 和 clientId 注册
     */
    void register(String sessionId, String clientId);

    // ==================== 会话属性 ====================

    /**
     * 设置属性
     */
    void setAttribute(Object name, Object value);

    /**
     * 获取属性
     */
    Object getAttribute(Object name);

    /**
     * 移除属性
     */
    Object removeAttribute(Object name);

    // ==================== 消息处理 ====================

    /**
     * 生成下一个消息流水号
     */
    int nextSerialNo();

    /**
     * 设置出站消息拦截器
     */
    void setOutBoundInterceptor(BiConsumer<TransportSession, JT808Message> outBoundInterceptor);

    /**
     * 处理响应消息（匹配等待中的请求）
     */
    boolean handleResponse(JT808Response message);

    // ==================== 生命周期 ====================

    /**
     * 更新最后访问时间
     */
    long updateLastAccessTime();

    /**
     * 销毁会话
     */
    void invalidate();

    /**
     * 获取传输类型
     */
    TransportType getTransportType();
}
