package com.lk.jtt808.protocol.annotation;

import com.lk.jtt808.protocol.entity.JT808Message;
import org.reflections.Reflections;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MessageHandlerRegistry {
    private static final Map<Integer, Class<? extends JT808Message>> messageMap = new ConcurrentHashMap<>();

    /**
     * 自动扫描包路径注册消息类
     */
    public static void autoRegister(String packagePath) throws IOException {
        Reflections reflections = new Reflections(packagePath);
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(MessageType.class);
        
        for (Class<?> clazz : annotatedClasses) {
            if (JT808Message.class.isAssignableFrom(clazz)) {
                MessageType annotation = clazz.getAnnotation(MessageType.class);
                register(annotation.value(), (Class<? extends JT808Message>) clazz);
            }
        }
    }

    /**
     * 手动注册消息类型
     */
    public static void register(int messageId, Class<? extends JT808Message> messageClass) {
        messageMap.put(messageId, messageClass);
    }

    /**
     * 根据消息ID获取对应的消息类
     */
    public static Class<? extends JT808Message> getMessageClass(int messageId) {
        return messageMap.get(messageId);
    }
}