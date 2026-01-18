package com.lk.jtt808.protocol.cache;

import com.lk.jtt808.protocol.annotation.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息元数据缓存管理器
 * 提供消息类元数据的获取和预热功能
 */
@Slf4j
public class MessageMetadataCache {
    private static final Map<Class<?>, MessageMetadata> CACHE = new ConcurrentHashMap<>();

    private MessageMetadataCache() {
    }

    /**
     * 获取或创建消息元数据
     */
    public static MessageMetadata getOrCreate(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, MessageMetadata::build);
    }

    /**
     * 预热缓存
     * 在应用启动时扫描指定包路径下的所有消息类并预先构建元数据
     *
     * @param packagePath 要扫描的包路径
     */
    public static void warmup(String packagePath) {
        log.info("预热消息元数据缓存: packagePath={}", packagePath);
        try {
            Reflections reflections = new Reflections(packagePath);
            Set<Class<?>> messageClasses = reflections.getTypesAnnotatedWith(MessageType.class);

            for (Class<?> clazz : messageClasses) {
                getOrCreate(clazz);
            }

            log.info("消息元数据缓存预热完成: 共缓存 {} 个消息类型", CACHE.size());
        } catch (Exception e) {
            log.error("消息元数据缓存预热失败", e);
        }
    }

    /**
     * 获取当前缓存大小
     */
    public static int getCacheSize() {
        return CACHE.size();
    }

    /**
     * 清空缓存（仅用于测试）
     */
    public static void clear() {
        CACHE.clear();
    }
}
