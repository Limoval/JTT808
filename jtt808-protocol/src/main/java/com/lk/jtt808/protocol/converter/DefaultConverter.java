package com.lk.jtt808.protocol.converter;

import com.lk.jtt808.protocol.cache.FieldMetadata;
import io.netty.buffer.ByteBuf;

/**
 * 默认转换器（不执行任何转换）
 */
public class DefaultConverter implements FieldConverter {
    @Override
    public Object decode(ByteBuf buf, FieldMetadata fieldMetadata) {
        return buf;
    }

    @Override
    public void encode(ByteBuf buf, Object value, FieldMetadata fieldMetadata) {
        // 默认不处理
    }
}
