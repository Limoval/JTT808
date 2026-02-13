package com.lk.jtt808.protocol.annotation.field;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定长/变长字符串字段注解
 * 对应 DataType.STRING
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface StringField {
    int length() default -1;  // -1表示读取剩余所有
    String charset() default "GBK";
    String desc() default "";
}
