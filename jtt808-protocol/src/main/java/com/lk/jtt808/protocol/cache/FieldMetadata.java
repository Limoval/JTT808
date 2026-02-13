package com.lk.jtt808.protocol.cache;

import com.lk.jtt808.protocol.annotation.MessageField;
import com.lk.jtt808.protocol.annotation.field.*;
import com.lk.jtt808.protocol.converter.DefaultConverter;
import com.lk.jtt808.protocol.converter.FieldConverter;
import com.lk.jtt808.protocol.entity.enums.DataType;
import lombok.Getter;

import java.lang.reflect.Field;

/**
 * 字段元数据缓存类
 * 缓存单个字段的反射信息和转换器实例
 */
@Getter
public class FieldMetadata {
    private final Field field;
    private final int order;
    private final DataType dataType;
    private final int length;
    private final String charset;
    private final String desc;
    private final FieldConverter converter;

    /** 兼容旧注解 - 保留原始 MessageField 引用 */
    private final MessageField legacyAnnotation;

    private FieldMetadata(Field field, int order, DataType dataType, int length,
                          String charset, String desc, FieldConverter converter,
                          MessageField legacyAnnotation) {
        this.field = field;
        this.order = order;
        this.dataType = dataType;
        this.length = length;
        this.charset = charset;
        this.desc = desc;
        this.converter = converter;
        this.legacyAnnotation = legacyAnnotation;
        // 预先设置可访问，避免每次解析时重复设置
        this.field.setAccessible(true);
    }

    /**
     * 兼容旧版构造函数
     */
    public FieldMetadata(Field field, MessageField annotation, FieldConverter converter) {
        this(field,
             annotation.order(),
             annotation.type(),
             annotation.length(),
             annotation.charset(),
             annotation.desc(),
             converter,
             annotation);
    }

    /**
     * 检查是否有自定义转换器
     */
    public boolean hasCustomConverter() {
        return converter != null;
    }

    /**
     * 获取注解（兼容旧代码）
     * @deprecated 使用具体的 getter 方法代替
     */
    @Deprecated
    public MessageField getAnnotation() {
        return legacyAnnotation;
    }

    // ===== 工厂方法 - 用于新的简化注解 =====

    public static FieldMetadata ofByte(Field field, int order, String desc) {
        return new FieldMetadata(field, order, DataType.BYTE, -1, "GBK", desc, null, null);
    }

    public static FieldMetadata ofWord(Field field, int order, String desc, FieldConverter converter) {
        return new FieldMetadata(field, order, DataType.WORD, -1, "GBK", desc, converter, null);
    }

    public static FieldMetadata ofDWord(Field field, int order, String desc, FieldConverter converter) {
        return new FieldMetadata(field, order, DataType.DWORD, -1, "GBK", desc, converter, null);
    }

    public static FieldMetadata ofBcd(Field field, int order, int length, String desc) {
        return new FieldMetadata(field, order, DataType.BCD, length, "GBK", desc, null, null);
    }

    public static FieldMetadata ofString(Field field, int order, int length, String charset, String desc) {
        return new FieldMetadata(field, order, DataType.STRING, length, charset, desc, null, null);
    }

    public static FieldMetadata ofBytes(Field field, int order, int length, String desc, FieldConverter converter) {
        return new FieldMetadata(field, order, DataType.BYTES, length, "GBK", desc, converter, null);
    }
}
