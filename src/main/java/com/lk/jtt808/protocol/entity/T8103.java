package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageField;
import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.converter.FieldConverter;
import com.lk.jtt808.protocol.entity.enums.DataType;
import com.lk.jtt808.utils.JT808;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@MessageType(JT808.设置终端参数)
@ToString(callSuper = true)
@Data
public class T8103 extends JT808Message {

    @MessageField(order = 1, type = DataType.BYTE, desc = "参数总数")
    private int parameterCount;

    @MessageField(order = 2, type = DataType.BYTES, desc = "参数项列表", 
                  converter = TerminalParameterListConverter.class)
    private List<TerminalParameter> parameters;

    @Data
    @ToString
    public static class TerminalParameter {
        @MessageField(order = 1, type = DataType.DWORD, desc = "参数ID")
        private long parameterId;

        @MessageField(order = 2, type = DataType.BYTE, desc = "参数长度")
        private int parameterLength;

        @MessageField(order = 3, type = DataType.BYTES, desc = "参数值")
        private byte[] parameterValue;

    }

    public static class TerminalParameterListConverter implements FieldConverter {

        @Override
        public Object decode(ByteBuf buf, MessageField annotation) {
            List<TerminalParameter> parameters = new ArrayList<>();

            while (buf.readableBytes() > 0) {
                if (buf.readableBytes() < 5) break; // 至少需要4字节ID + 1字节长度

                TerminalParameter parameter = new TerminalParameter();

                // 参数ID（DWORD）
                parameter.setParameterId(buf.readUnsignedInt());

                // 参数长度（BYTE）
                int length = buf.readUnsignedByte();
                parameter.setParameterLength(length);

                // 检查剩余字节
                if (buf.readableBytes() < length) {
                    throw new IllegalArgumentException("Invalid parameter length: " + length);
                }

                // 参数值
                byte[] value = new byte[length];
                buf.readBytes(value);
                parameter.setParameterValue(value);

                parameters.add(parameter);
            }

            return parameters;
        }

        @Override
        public void encode(ByteBuf buf, Object value, MessageField annotation) {
            if (!(value instanceof List)) {
                throw new IllegalArgumentException("Value must be List<TerminalParameter>");
            }

            @SuppressWarnings("unchecked")
            List<TerminalParameter> parameters = (List<TerminalParameter>) value;

            for (TerminalParameter parameter : parameters) {
                // 参数ID（DWORD）
                buf.writeInt((int) parameter.getParameterId());

                // 参数长度（BYTE）
                byte[] paramValue = parameter.getParameterValue();
                int length = paramValue != null ? paramValue.length : 0;
                buf.writeByte(length);

                // 参数值
                if (paramValue != null && length > 0) {
                    buf.writeBytes(paramValue);
                }
            }
        }
    }
}