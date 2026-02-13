package com.lk.jtt808.protocol.annotation.field;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 1字节无符号整数字段注解
 * 对应 DataType.BYTE
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ByteField {
    String desc() default "";
}
