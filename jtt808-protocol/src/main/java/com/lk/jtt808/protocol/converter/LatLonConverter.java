package com.lk.jtt808.protocol.converter;

import com.lk.jtt808.protocol.cache.FieldMetadata;
import io.netty.buffer.ByteBuf;

/**
 * 经纬度转换器
 * JT808协议中经纬度以整数形式传输，单位为 1/10^6 度
 */
public class LatLonConverter implements FieldConverter {
    @Override
    public Object decode(ByteBuf buf, FieldMetadata fieldMetadata) {
        long value = buf.readUnsignedInt();
        return value / 1000000.0;
    }

    @Override
    public void encode(ByteBuf buf, Object value, FieldMetadata fieldMetadata) {
        double dValue = (double) value;
        buf.writeInt((int) (dValue * 1000000));
    }
}
