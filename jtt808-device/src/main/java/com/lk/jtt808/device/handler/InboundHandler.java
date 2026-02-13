package com.lk.jtt808.device.handler;


import com.lk.jtt808.device.transport.TransportSession;
import com.lk.jtt808.protocol.entity.JT808Message;

/**
 * JTT808消息请求回复处理器
 */
public interface InboundHandler<T extends JT808Message> {

    /**
     * 消息处理
     *
     * @param message 消息
     * @param session 传输会话
     * @return 处理结果码
     */
    Integer handle(T message, TransportSession session);

    /**
     * 回复
     *
     * @param message 消息
     * @param code    响应码
     */
    void reply(T message, Integer code);

    /**
     * 处理消息的类型
     */
    Class<T> getHandleType();
}
