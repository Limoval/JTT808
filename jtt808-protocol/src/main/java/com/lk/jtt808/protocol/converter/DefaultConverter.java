package com.lk.jtt808.protocol.converter;

import com.lk.jtt808.protocol.annotation.MessageField;
import io.netty.buffer.ByteBuf;

public class DefaultConverter implements FieldConverter {
    @Override
    public Object decode(ByteBuf buf, MessageField field) {
        return buf;
    }
    @Override
    public void encode(ByteBuf buf, Object value, MessageField field) {

    }
}