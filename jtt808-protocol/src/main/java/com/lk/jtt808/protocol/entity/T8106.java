package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.CustomMapping;
import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.constant.JT808;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询指定终端参数
 * 消息ID: 0x8106
 */
@MessageType(JT808.查询指定终端参数)
@EqualsAndHashCode(callSuper = true)
@Data
public class T8106 extends JT808Message implements CustomMapping {

    private int parameterCount;

    private List<Long> parameterIds = new ArrayList<>();

    @Override
    public boolean customParse(ByteBuf buf) {
        if (buf.readableBytes() < 1) {
            throw new IllegalArgumentException("T8106 missing parameter count");
        }
        parameterCount = buf.readUnsignedByte();
        parameterIds = new ArrayList<>(parameterCount);
        for (int i = 0; i < parameterCount; i++) {
            if (buf.readableBytes() < 4) {
                throw new IllegalArgumentException("T8106 missing parameter id at index " + i);
            }
            parameterIds.add(buf.readUnsignedInt());
        }
        if (buf.isReadable()) {
            throw new IllegalArgumentException("Unread bytes remain after parsing T8106: " + buf.readableBytes());
        }
        return true;
    }

    @Override
    public boolean customEncode(ByteBuf buf) {
        int count = parameterIds == null ? parameterCount : parameterIds.size();
        if (count < 0 || count > 0xFF) {
            throw new IllegalArgumentException("T8106 parameter count out of range: " + count);
        }
        buf.writeByte(count);
        if (parameterIds != null) {
            for (Long parameterId : parameterIds) {
                if (parameterId == null || parameterId < 0 || parameterId > 0xFFFF_FFFFL) {
                    throw new IllegalArgumentException("Invalid T8106 parameter id: " + parameterId);
                }
                buf.writeInt(parameterId.intValue());
            }
        }
        return true;
    }
}
