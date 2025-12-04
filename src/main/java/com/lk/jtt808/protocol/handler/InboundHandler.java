package com.lk.jtt808.protocol.handler;


import com.lk.jtt808.protocol.entity.JT808Message;
import com.lk.jtt808.protocol.session.Session;

/**
 * @author: Limoval
 * time: 2025/6/9 17:21 周一
 * description: JTT808消息请求回复处理器
 */
public interface InboundHandler<T extends JT808Message> {

    /**
     * 消息处理
     *
     * @param message
     * @return todo 后续可能根据返回值复杂程度修改返回值类型
     */
    Integer handle(T message, Session session);

    /**
     * 回复
     *
     * @param message
     * @param code
     */
    void reply(T message, Integer code);


    /**
     * 处理消息的类型
     * @return
     */
    Class<T> getHandleType();


}
