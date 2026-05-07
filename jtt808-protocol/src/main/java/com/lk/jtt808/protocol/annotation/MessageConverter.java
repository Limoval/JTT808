package com.lk.jtt808.protocol.annotation;

import com.lk.jtt808.protocol.cache.FieldMetadata;
import com.lk.jtt808.protocol.cache.MessageMetadata;
import com.lk.jtt808.protocol.cache.MessageMetadataCache;
import com.lk.jtt808.protocol.entity.enums.DataType;
import com.lk.jtt808.protocol.util.BcdUtil;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;

@Slf4j
public class MessageConverter {
    public static <T> T parse(ByteBuf buf, Class<T> clazz) throws Exception {
        T instance = clazz.getDeclaredConstructor().newInstance();

        // 优先检查是否实现了自定义解析接口
        if (instance instanceof CustomMapping customParser) {
            if (customParser.customParse(buf)) {
                log.debug("使用自定义解析逻辑: {}", instance.getClass().getSimpleName());
                return instance;
            }
            log.error("自定义解析失败，回退到默认解析逻辑: {}", instance.getClass().getSimpleName());
        }

        // 使用缓存的元数据（已预排序）
        MessageMetadata metadata = MessageMetadataCache.getOrCreate(clazz);

        for (FieldMetadata fm : metadata.getOrderedFields()) {
            // 使用缓存的转换器
            if (fm.hasCustomConverter()) {
                Object value = fm.getConverter().decode(buf, fm);
                fm.getField().set(instance, value);
                continue;
            }

            // 标准类型处理
            DataType dataType = fm.getDataType();
            switch (dataType) {
                case BYTE:
                    ensureReadable(buf, 1, fm);
                    fm.getField().set(instance, buf.readUnsignedByte());
                    break;
                case WORD:
                    ensureReadable(buf, 2, fm);
                    fm.getField().set(instance, buf.readUnsignedShort());
                    break;
                case DWORD:
                    ensureReadable(buf, 4, fm);
                    fm.getField().set(instance, buf.readUnsignedInt());
                    break;
                case BCD:
                    ensurePositiveLength(fm);
                    ensureReadable(buf, fm.getLength(), fm);
                    fm.getField().set(instance, readBcd(buf, fm.getLength()));
                    break;
                case STRING:
                    fm.getField().set(instance, readString(buf, fm.getLength(), fm.getCharset()));
                    break;
                case BYTES:
                    fm.getField().set(instance, readBytes(buf, fm.getLength()));
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported field type for annotation parsing: "
                            + dataType + ", field=" + fm.getField().getName());
            }
        }
        if (buf.isReadable()) {
            throw new IllegalArgumentException("Unread bytes remain after parsing "
                    + clazz.getSimpleName() + ": " + buf.readableBytes());
        }
        return instance;
    }

    private static String readBcd(ByteBuf buf, int length) {
        byte[] bcdBytes = new byte[length];
        buf.readBytes(bcdBytes);
        return BcdUtil.bcdToString(bcdBytes);
    }

    private static String readString(ByteBuf buf, int length, String charset) {
        int len = length > 0 ? length : buf.readableBytes();
        ensureReadable(buf, len, null);
        return buf.readCharSequence(len, Charset.forName(charset)).toString();
    }

    private static byte[] readBytes(ByteBuf buf, int length) {
        int len = length > 0 ? length : buf.readableBytes();
        ensureReadable(buf, len, null);
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return bytes;
    }


    /**
     * 将实体对象转换为ByteBuf
     * @param bodyBuf 目标ByteBuf
     * @param msg 源实体对象
     * @param <T> 实体类型
     * @throws Exception 转换异常
     */
    public static <T> void toByteBuf(ByteBuf bodyBuf, T msg) throws Exception {
        if (bodyBuf == null || msg == null) {
            throw new IllegalArgumentException("ByteBuf and message object cannot be null");
        }

        // 优先检查是否实现了自定义编码接口
        if (msg instanceof CustomMapping customParser) {
            if (customParser.customEncode(bodyBuf)) {
                log.debug("使用自定义编码逻辑: {}", msg.getClass().getSimpleName());
                return;
            }
            log.error("自定义编码失败，回退到默认编码逻辑: {}", msg.getClass().getSimpleName());
        }

        // 使用缓存的元数据（已预排序）
        MessageMetadata metadata = MessageMetadataCache.getOrCreate(msg.getClass());

        for (FieldMetadata fm : metadata.getOrderedFields()) {
            Object fieldValue = fm.getField().get(msg);

            if (fieldValue == null) {
                throw new IllegalArgumentException("Required field cannot be null: "
                        + msg.getClass().getSimpleName() + "." + fm.getField().getName());
            }

            // 使用缓存的转换器
            if (fm.hasCustomConverter()) {
                fm.getConverter().encode(bodyBuf, fieldValue, fm);
                continue;
            }

            // 处理标准字段类型
            writeFieldToByteBuf(bodyBuf, fm, fieldValue);
        }
    }

    /**
     * 根据字段类型将值写入ByteBuf
     * @param bodyBuf 目标ByteBuf
     * @param fm 字段元数据
     * @param fieldValue 字段值
     * @throws IllegalArgumentException 类型转换异常
     */
    private static void writeFieldToByteBuf(ByteBuf bodyBuf, FieldMetadata fm, Object fieldValue) {
        try {
            switch (fm.getDataType()) {
                case BYTE:
                    writeByte(bodyBuf, fieldValue);
                    break;
                case WORD:
                    writeWord(bodyBuf, fieldValue);
                    break;
                case DWORD:
                    writeDWord(bodyBuf, fieldValue);
                    break;
                case BCD:
                    writeBcd(bodyBuf, fieldValue, fm);
                    break;
                case STRING:
                    writeString(bodyBuf, fieldValue, fm.getLength(), fm.getCharset());
                    break;
                case BYTES:
                    writeBytes(bodyBuf, fieldValue, fm);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported field type: " + fm.getDataType());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to write field of type: " + fm.getDataType(), e);
        }
    }

    /**
     * 写入字节值
     */
    private static void writeByte(ByteBuf bodyBuf, Object value) {
        if (value instanceof Number number) {
            int intValue = number.intValue();
            if (intValue < 0 || intValue > 0xFF) {
                throw new IllegalArgumentException("BYTE value out of range: " + intValue);
            }
            bodyBuf.writeByte(intValue);
        } else {
            throw new IllegalArgumentException("BYTE field must be Number type, but got: " +
                    value.getClass().getSimpleName());
        }
    }

    /**
     * 写入短整型值
     */
    private static void writeWord(ByteBuf bodyBuf, Object value) {
        if (value instanceof Number number) {
            int intValue = number.intValue();
            if (intValue < 0 || intValue > 0xFFFF) {
                throw new IllegalArgumentException("WORD value out of range: " + intValue);
            }
            bodyBuf.writeShort(intValue);
        } else {
            throw new IllegalArgumentException("WORD field must be Number type, but got: " +
                    value.getClass().getSimpleName());
        }
    }

    /**
     * 写入双字值
     */
    private static void writeDWord(ByteBuf bodyBuf, Object value) {
        if (value instanceof Number number) {
            long longValue = number.longValue();
            if (longValue < 0 || longValue > 0xFFFF_FFFFL) {
                throw new IllegalArgumentException("DWORD value out of range: " + longValue);
            }
            bodyBuf.writeInt((int) longValue);
        } else {
            throw new IllegalArgumentException("DWORD field must be Number type, but got: " +
                    value.getClass().getSimpleName());
        }
    }

    /**
     * 写入BCD编码字符串
     */
    private static void writeBcd(ByteBuf bodyBuf, Object value, FieldMetadata fm) {
        if (!(value instanceof String bcdString)) {
            throw new IllegalArgumentException("BCD field must be String type, but got: " +
                    value.getClass().getSimpleName());
        }

        ensurePositiveLength(fm);
        try {
            byte[] bcdBytes = BcdUtil.stringToBcd(bcdString, fm.getLength());
            bodyBuf.writeBytes(bcdBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode BCD string: " + bcdString, e);
        }
    }

    /**
     * 写入字符串
     */
    private static void writeString(ByteBuf bodyBuf, Object value, int length, String charsetName) {
        if (!(value instanceof String stringValue)) {
            throw new IllegalArgumentException("STRING field must be String type, but got: " +
                    value.getClass().getSimpleName());
        }

        try {
            Charset charset = Charset.forName(charsetName);
            byte[] bytes = stringValue.getBytes(charset);
            if (length > 0) {
                if (bytes.length > length) {
                    throw new IllegalArgumentException("STRING field too long: field="
                            + value.getClass().getSimpleName() + ", expectedBytes=" + length
                            + ", actualBytes=" + bytes.length);
                }
                bodyBuf.writeBytes(bytes);
                if (bytes.length < length) {
                    bodyBuf.writeZero(length - bytes.length);
                }
            } else {
                bodyBuf.writeBytes(bytes);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode string with charset: " + charsetName, e);
        }
    }

    /**
     * 写入字节数组
     */
    private static void writeBytes(ByteBuf bodyBuf, Object value, FieldMetadata fm) {
        if (!(value instanceof byte[] bytes)) {
            throw new IllegalArgumentException("BYTES field must be byte[] type, but got: " +
                    value.getClass().getSimpleName());
        }

        int length = fm.getLength();
        if (length > 0) {
            if (bytes.length > length) {
                throw new IllegalArgumentException("BYTES field too long: field="
                        + fm.getField().getName() + ", expectedBytes=" + length
                        + ", actualBytes=" + bytes.length);
            }
            bodyBuf.writeBytes(bytes);
            if (bytes.length < length) {
                bodyBuf.writeZero(length - bytes.length);
            }
        } else {
            bodyBuf.writeBytes(bytes);
        }
    }

    private static void ensureReadable(ByteBuf buf, int requiredBytes, FieldMetadata fm) {
        if (requiredBytes < 0) {
            throw new IllegalArgumentException("Required bytes cannot be negative: " + requiredBytes);
        }
        if (buf.readableBytes() < requiredBytes) {
            String fieldName = fm == null ? "<dynamic>" : fm.getField().getName();
            throw new IllegalArgumentException("Insufficient readable bytes for field " + fieldName
                    + ": required=" + requiredBytes + ", actual=" + buf.readableBytes());
        }
    }

    private static void ensurePositiveLength(FieldMetadata fm) {
        if (fm.getLength() <= 0) {
            throw new IllegalArgumentException("Field requires positive fixed length: "
                    + fm.getField().getName() + ", type=" + fm.getDataType()
                    + ", length=" + fm.getLength());
        }
    }

}
