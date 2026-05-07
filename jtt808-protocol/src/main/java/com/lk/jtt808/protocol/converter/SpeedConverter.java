package com.lk.jtt808.protocol.converter;

import com.lk.jtt808.protocol.cache.FieldMetadata;
import io.netty.buffer.ByteBuf;

/**
 * 速度转换器
 * JT808协议中速度以整数形式传输，单位为 1/10 km/h
 */
public class SpeedConverter implements FieldConverter {
    @Override
    public Object decode(ByteBuf buf, FieldMetadata fieldMetadata) {
        return buf.readUnsignedShort();
    }

    @Override
    public void encode(ByteBuf buf, Object value, FieldMetadata fieldMetadata) {
        int intValue = ((Number) value).intValue();
        if (intValue < 0 || intValue > 0xFFFF) {
            throw new IllegalArgumentException("Speed value out of range: " + intValue);
        }
        buf.writeShort(intValue);
    }
}
