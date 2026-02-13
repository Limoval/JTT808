package com.lk.jtt808.device.handler;


import com.lk.jtt808.device.transport.TransportSession;
import com.lk.jtt808.protocol.entity.JT808Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通过消息类型向下分发消息
 */
@Component
@Slf4j
public class MessageHandlerDispatcher implements ApplicationContextAware {

    /**
     * 存储消息类型和Handler映射
     */
    private final Map<Class<? extends JT808Message>, InboundHandler<? extends JT808Message>> handlerMap = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, InboundHandler> handlers = applicationContext.getBeansOfType(InboundHandler.class);
        handlers.values().forEach(handler -> {
            Class handleType = handler.getHandleType();
            handlerMap.put(handleType, handler);
            log.info("注册处理器: {} -> {}", handleType.getSimpleName(), handler.getClass().getSimpleName());
        });
    }

    /**
     * 将消息派遣到对应的handler中
     */
    public <T extends JT808Message> void dispatch(T msg, TransportSession session) {
        Class<? extends JT808Message> actualType = msg.getClass();

        InboundHandler<T> handler = (InboundHandler<T>) handlerMap.get(actualType);

        if (handler == null) {
            throw new UnsupportedOperationException("不支持的消息类型: " + actualType.getSimpleName());
        }

        handler.handle(msg, session);
    }
}
