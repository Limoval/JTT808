package com.lk.jtt808.protocol.converter;

import com.lk.jtt808.protocol.cache.FieldMetadata;
import com.lk.jtt808.protocol.entity.T0200;
import io.netty.buffer.ByteBuf;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 附加信息转换器
 * 解析T0200位置信息中的附加信息列表
 */
@Component
public class AdditionalInfoConverter implements FieldConverter {

    @Override
    public Object decode(ByteBuf buf, FieldMetadata fieldMetadata) {
        List<T0200.AdditionalInfo> infos = new ArrayList<>();

        while (buf.readableBytes() > 0) {
            if (buf.readableBytes() < 2) break;

            int id = buf.readUnsignedByte();
            int length = buf.readUnsignedByte();

            if (buf.readableBytes() < length) break;

            byte[] content = new byte[length];
            buf.readBytes(content);

            T0200.AdditionalInfo info = new T0200.AdditionalInfo();
            info.setId(id);
            info.setLength(length);
            info.setContent(content);
            infos.add(info);
        }

        return infos;
    }

    @Override
    public void encode(ByteBuf buf, Object value, FieldMetadata fieldMetadata) {
        if (!(value instanceof List)) return;

        @SuppressWarnings("unchecked")
        List<T0200.AdditionalInfo> infos = (List<T0200.AdditionalInfo>) value;

        for (T0200.AdditionalInfo info : infos) {
            buf.writeByte(info.getId());
            buf.writeByte(info.getLength());
            buf.writeBytes(info.getContent());
        }
    }
}
