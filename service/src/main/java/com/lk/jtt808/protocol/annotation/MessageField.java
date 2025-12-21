package com.lk.jtt808.protocol.annotation;


import com.lk.jtt808.protocol.converter.DefaultConverter;
import com.lk.jtt808.protocol.converter.FieldConverter;
import com.lk.jtt808.protocol.entity.enums.DataType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MessageField {
    int order();         // 解析顺序

    DataType type();     // 数据类型

    int length() default -1;  // 特殊长度（用于STRING/BYTES）

    String charset() default "GBK"; // 字符编码

    String desc() default "";

    Class<? extends FieldConverter> converter() default DefaultConverter.class;

    int[] versions() default {};    //支持协议版本

}