package com.lk.jtt808.protocol.converter;

import com.lk.jtt808.protocol.annotation.MessageField;
import io.netty.buffer.ByteBuf;

public interface FieldConverter {
    Object decode(ByteBuf buf, MessageField field);
    void encode(ByteBuf buf, Object value, MessageField field);
}