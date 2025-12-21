package com.lk.jtt808.protocol.converter;

import com.lk.jtt808.protocol.annotation.MessageField;
import io.netty.buffer.ByteBuf;

/**
 * @author: Limoval
 * time: 2025/5/26 14:57 周一
 * description:
 */
public class SpeedConverter implements FieldConverter {
    @Override
    public Object decode(ByteBuf buf, MessageField field) {
        int i = buf.readUnsignedShort();
        return i / 10;
    }

    @Override
    public void encode(ByteBuf buf, Object value, MessageField field) {
        double dValue = (double) value;
        buf.writeInt((int) (dValue * 10));
    }
}
