package com.lk.jtt808.protocol.cache;

import com.lk.jtt808.protocol.annotation.MessageField;
import com.lk.jtt808.protocol.converter.FieldConverter;
import lombok.Getter;

import java.lang.reflect.Field;

/**
 * 字段元数据缓存类
 * 缓存单个字段的反射信息和转换器实例
 */
@Getter
public class FieldMetadata {
    private final Field field;
    private final MessageField annotation;
    private final FieldConverter converter;

    public FieldMetadata(Field field, MessageField annotation, FieldConverter converter) {
        this.field = field;
        this.annotation = annotation;
        this.converter = converter;
        // 预先设置可访问，避免每次解析时重复设置
        this.field.setAccessible(true);
    }

    /**
     * 检查是否有自定义转换器
     */
    public boolean hasCustomConverter() {
        return converter != null;
    }
}
