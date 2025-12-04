package com.lk.jtt808.protocol.handler;


import com.lk.jtt808.protocol.entity.JT808Message;
import com.lk.jtt808.protocol.session.Session;
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
     * key: 消息类型
     * value: handler
     */
    private final Map<Class<? extends JT808Message>, InboundHandler<? extends JT808Message>> handlerMap = new ConcurrentHashMap<>();


    /**
     * 从spring中获取所有的InboundHandler,根据handler.getHandleType()方法获取对应的消息类型,然后装入handlerMap中
     * @param applicationContext the ApplicationContext object to be used by this object
     * @throws BeansException
     */
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
     * 根据消息对象实际类型，从映射表中获取对应的handler，并调用handler的handle方法处理消息
     * @param msg
     * @param session
     * @param <T>
     */
    public <T extends JT808Message> void dispatch(T msg, Session session) {
        //1.获取消息对象的实际类型
        Class<? extends JT808Message> actualType = msg.getClass();

        //2.从映射表中查找对应的handler
        InboundHandler<T> handler = (InboundHandler<T>) handlerMap.get(actualType);

        //3.如果找不到handler，抛出异常
        if (handler == null) {
            throw new UnsupportedOperationException("不支持的消息类型: " + actualType.getSimpleName());
        }

        //4.调用handler的handle方法
        handler.handle(msg, session);
    }
}
