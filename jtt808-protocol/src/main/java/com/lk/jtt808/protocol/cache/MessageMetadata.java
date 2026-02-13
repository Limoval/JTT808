package com.lk.jtt808.protocol.cache;

import com.lk.jtt808.protocol.annotation.MessageField;
import com.lk.jtt808.protocol.annotation.field.*;
import com.lk.jtt808.protocol.converter.DefaultConverter;
import com.lk.jtt808.protocol.converter.FieldConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;
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
     * 支持新的简化注解和旧的 @MessageField 注解
     */
    public static MessageMetadata build(Class<?> clazz) {
        List<FieldMetadata> fieldMetadataList = new ArrayList<>();
        int autoOrder = 1;

        // 收集类层次结构中的所有字段（从父类开始）
        List<Field> allFields = collectAllFields(clazz);

        for (Field field : allFields) {
            // 1. 检查新的简化注解
            FieldMetadata fm = parseSimplifiedAnnotation(field, autoOrder);
            if (fm != null) {
                fieldMetadataList.add(fm);
                autoOrder++;
                continue;
            }

            // 2. 兼容旧的 @MessageField 注解
            MessageField annotation = field.getAnnotation(MessageField.class);
            if (annotation != null) {
                FieldConverter converter = null;
                Class<? extends FieldConverter> converterClass = annotation.converter();
                if (converterClass != null && !converterClass.equals(DefaultConverter.class)) {
                    converter = getOrCreateConverter(converterClass);
                }
                fieldMetadataList.add(new FieldMetadata(field, annotation, converter));
            }
        }

        // 按 order 排序
        fieldMetadataList.sort(Comparator.comparingInt(FieldMetadata::getOrder));

        log.debug("构建消息元数据: class={}, fieldCount={}", clazz.getSimpleName(), fieldMetadataList.size());
        return new MessageMetadata(clazz, fieldMetadataList);
    }

    /**
     * 收集类层次结构中的所有声明字段（从父类开始，按声明顺序）
     */
    private static List<Field> collectAllFields(Class<?> clazz) {
        List<Class<?>> hierarchy = new ArrayList<>();

        // 构建类层次结构（不包括 Object）
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            hierarchy.add(current);
            current = current.getSuperclass();
        }

        // 反转，从父类开始处理
        Collections.reverse(hierarchy);

        // 收集所有字段
        List<Field> allFields = new ArrayList<>();
        for (Class<?> c : hierarchy) {
            allFields.addAll(Arrays.asList(c.getDeclaredFields()));
        }

        return allFields;
    }

    /**
     * 解析简化注解
     * @return 如果字段有简化注解则返回 FieldMetadata，否则返回 null
     */
    private static FieldMetadata parseSimplifiedAnnotation(Field field, int autoOrder) {
        if (field.isAnnotationPresent(ByteField.class)) {
            ByteField anno = field.getAnnotation(ByteField.class);
            return FieldMetadata.ofByte(field, autoOrder, anno.desc());
        }

        if (field.isAnnotationPresent(WordField.class)) {
            WordField anno = field.getAnnotation(WordField.class);
            FieldConverter converter = null;
            if (!anno.converter().equals(DefaultConverter.class)) {
                converter = getOrCreateConverter(anno.converter());
            }
            return FieldMetadata.ofWord(field, autoOrder, anno.desc(), converter);
        }

        if (field.isAnnotationPresent(DWordField.class)) {
            DWordField anno = field.getAnnotation(DWordField.class);
            FieldConverter converter = null;
            if (!anno.converter().equals(DefaultConverter.class)) {
                converter = getOrCreateConverter(anno.converter());
            }
            return FieldMetadata.ofDWord(field, autoOrder, anno.desc(), converter);
        }

        if (field.isAnnotationPresent(BcdField.class)) {
            BcdField anno = field.getAnnotation(BcdField.class);
            return FieldMetadata.ofBcd(field, autoOrder, anno.length(), anno.desc());
        }

        if (field.isAnnotationPresent(StringField.class)) {
            StringField anno = field.getAnnotation(StringField.class);
            return FieldMetadata.ofString(field, autoOrder, anno.length(), anno.charset(), anno.desc());
        }

        if (field.isAnnotationPresent(BytesField.class)) {
            BytesField anno = field.getAnnotation(BytesField.class);
            FieldConverter converter = null;
            if (!anno.converter().equals(DefaultConverter.class)) {
                converter = getOrCreateConverter(anno.converter());
            }
            return FieldMetadata.ofBytes(field, autoOrder, anno.length(), anno.desc(), converter);
        }

        return null;
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
