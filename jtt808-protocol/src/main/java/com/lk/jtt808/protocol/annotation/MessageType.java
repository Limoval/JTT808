package com.lk.jtt808.protocol.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MessageType {
    /**
     * messageId
     */
    int value();

    /**
     * 支持的协议版本（默认支持2013版）
     */
    int version() default 0;
}