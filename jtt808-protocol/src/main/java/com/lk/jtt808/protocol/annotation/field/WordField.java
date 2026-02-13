package com.lk.jtt808.protocol.annotation.field;

import com.lk.jtt808.protocol.converter.DefaultConverter;
import com.lk.jtt808.protocol.converter.FieldConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 2字节无符号整数字段注解
 * 对应 DataType.WORD
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface WordField {
    String desc() default "";
    Class<? extends FieldConverter> converter() default DefaultConverter.class;
}
