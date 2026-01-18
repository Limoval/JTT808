package com.lk.jtt808.protocol.cache;

import com.lk.jtt808.protocol.annotation.MessageField;
import com.lk.jtt808.protocol.converter.DefaultConverter;
import com.lk.jtt808.protocol.converter.FieldConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息元数据缓存类
 * 缓存消息类的所有字段元数据，包括预排序的字段列表和转换器实例
 */
@Slf4j
@Getter
public class MessageMetadata {
    private final Class<?> messageClass;
    private final List<FieldMetadata> orderedFields;

    /** 转换器实例缓存（避免重复创建） */
    private static final Map<Class<? extends FieldConverter>, FieldConverter> CONVERTER_CACHE = new ConcurrentHashMap<>();

    private MessageMetadata(Class<?> messageClass, List<FieldMetadata> orderedFields) {
        this.messageClass = messageClass;
        this.orderedFields = orderedFields;
    }

    /**
     * 构建消息元数据
     * 一次性扫描、排序、缓存所有字段信息
     */
    public static MessageMetadata build(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        List<FieldMetadata> fieldMetadataList = new ArrayList<>();

        for (Field field : fields) {
            MessageField annotation = field.getAnnotation(MessageField.class);
            if (annotation == null) {
                continue;
            }

            FieldConverter converter = null;
            Class<? extends FieldConverter> converterClass = annotation.converter();
            if (converterClass != null && !converterClass.equals(DefaultConverter.class)) {
                converter = getOrCreateConverter(converterClass);
            }

            fieldMetadataList.add(new FieldMetadata(field, annotation, converter));
        }

        // 按order排序
        fieldMetadataList.sort(Comparator.comparingInt(fm -> fm.getAnnotation().order()));

        log.debug("构建消息元数据: class={}, fieldCount={}", clazz.getSimpleName(), fieldMetadataList.size());
        return new MessageMetadata(clazz, fieldMetadataList);
    }

    /**
     * 获取或创建转换器实例（缓存）
     */
    private static FieldConverter getOrCreateConverter(Class<? extends FieldConverter> converterClass) {
        return CONVERTER_CACHE.computeIfAbsent(converterClass, clazz -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                log.error("创建转换器实例失败: {}", clazz.getName(), e);
                throw new RuntimeException("Failed to create converter: " + clazz.getName(), e);
            }
        });
    }
}
