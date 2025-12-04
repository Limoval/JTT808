package com.lk.jtt808.protocol.annotation;

import io.netty.buffer.ByteBuf;

/**
 * 自定义映射接口
 * 实现此接口的消息类可以自定义解析逻辑
 */
public interface CustomMapping {
    
    /**
     * 自定义解析方法
     * @param buf 数据缓冲区
     * @return true表示解析成功，false表示使用默认解析逻辑
     */
    boolean customParse(ByteBuf buf);
    
    /**
     * 自定义编码方法
     * @param buf 目标缓冲区
     * @return true表示编码成功，false表示使用默认编码逻辑
     */
    default boolean customEncode(ByteBuf buf) {
        return false;
    }
}