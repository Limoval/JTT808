package com.lk.jtt808.protocol.converter;

import com.lk.jtt808.protocol.annotation.MessageField;
import io.netty.buffer.ByteBuf;

public class LatLonConverter implements FieldConverter {
    @Override
    public Object decode(ByteBuf buf, MessageField field) {
        long value = buf.readUnsignedInt();
        return value / 1000000.0;
    }
    @Override
    public void encode(ByteBuf buf, Object value, MessageField field) {
        double dValue = (double) value;
        buf.writeInt((int) (dValue * 1000000));
    }
}