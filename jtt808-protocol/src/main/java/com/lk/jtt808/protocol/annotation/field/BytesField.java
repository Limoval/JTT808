package com.lk.jtt808.protocol.annotation.field;

import com.lk.jtt808.protocol.converter.DefaultConverter;
import com.lk.jtt808.protocol.converter.FieldConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 原始字节数组字段注解
 * 对应 DataType.BYTES
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BytesField {
    int length() default -1;  // -1表示读取剩余所有
    String desc() default "";
    Class<? extends FieldConverter> converter() default DefaultConverter.class;
}
