package com.lk.jtt808.protocol.converter;

import com.lk.jtt808.protocol.cache.FieldMetadata;
import io.netty.buffer.ByteBuf;

/**
 * 字段转换器接口
 * 用于自定义字段的编解码逻辑
 */
public interface FieldConverter {
    /**
     * 解码：从ByteBuf中读取数据并转换为Java对象
     * @param buf 数据缓冲区
     * @param fieldMetadata 字段元数据
     * @return 解码后的对象
     */
    Object decode(ByteBuf buf, FieldMetadata fieldMetadata);

    /**
     * 编码：将Java对象写入ByteBuf
     * @param buf 数据缓冲区
     * @param value 待编码的值
     * @param fieldMetadata 字段元数据
     */
    void encode(ByteBuf buf, Object value, FieldMetadata fieldMetadata);
}
