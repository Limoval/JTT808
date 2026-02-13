package com.lk.jtt808.protocol.annotation.field;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * BCD编码字符串字段注解
 * 对应 DataType.BCD
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BcdField {
    int length();  // BCD字节长度
    String desc() default "";
}
