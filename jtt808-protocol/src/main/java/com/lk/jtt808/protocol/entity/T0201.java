package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.CustomMapping;
import com.lk.jtt808.protocol.annotation.MessageConverter;
import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.constant.JT808;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 位置信息查询应答
 * 消息ID: 0x0201
 */
@MessageType(JT808.位置信息查询应答)
@EqualsAndHashCode(callSuper = true)
@Data
public class T0201 extends JT808Message implements JT808Response, CustomMapping {

    private int responseSerialNo;

    private T0200 location;

    @Override
    public boolean customParse(ByteBuf buf) {
        if (buf.readableBytes() < 2) {
            throw new IllegalArgumentException("T0201 missing response serial number");
        }
        responseSerialNo = buf.readUnsignedShort();
        try {
            location = MessageConverter.parse(buf, T0200.class);
            return true;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse T0201 location body", e);
        }
    }

    @Override
    public boolean customEncode(ByteBuf buf) {
        if (location == null) {
            throw new IllegalArgumentException("T0201 location cannot be null");
        }
        buf.writeShort(responseSerialNo);
        try {
            MessageConverter.toByteBuf(buf, location);
            return true;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to encode T0201 location body", e);
        }
    }
}
