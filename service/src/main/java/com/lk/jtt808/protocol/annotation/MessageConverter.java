package com.lk.jtt808.protocol.annotation;


import com.lk.jtt808.protocol.converter.DefaultConverter;
import com.lk.jtt808.protocol.converter.FieldConverter;
import com.lk.jtt808.utils.BcdUtil;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

@Slf4j
public class MessageConverter {
    public static <T> T parse(ByteBuf buf, Class<T> clazz) throws Exception {
        T instance = clazz.getDeclaredConstructor().newInstance();
        Field[] fields = clazz.getDeclaredFields();

        // 优先检查是否实现了自定义解析接口
        if (instance instanceof CustomMapping customParser) {
            if (customParser.customParse(buf)) {
                log.info("使用自定义解析逻辑: {}", instance.getClass().getSimpleName());
                return instance;
            }
            log.error("自定义解析失败，回退到默认解析逻辑: {}", instance.getClass().getSimpleName());
        }

        // 按order排序字段
        Arrays.sort(fields, Comparator.comparingInt(f ->
                Optional.ofNullable(f.getAnnotation(MessageField.class))
                        .map(MessageField::order)
                        .orElse(Integer.MAX_VALUE)));

        for (Field field : fields) {
            MessageField anno = field.getAnnotation(MessageField.class);
            if (anno == null) continue;
            field.setAccessible(true);

            if (decodeFieldWithConverter(buf, field, anno, instance)) continue;

            switch (anno.type()) {
                case BYTE:
                    setFieldValue(field, instance, buf.readUnsignedByte());
                    break;
                case WORD:
                    setFieldValue(field, instance, buf.readUnsignedShort());
                    break;
                case DWORD:
                    setFieldValue(field, instance, buf.readUnsignedInt());
                    break;
                case BCD:
                    setFieldValue(field, instance, readBcd(buf, anno.length()));
                    break;
                case STRING:
                    setFieldValue(field, instance, readString(buf, anno.length(), anno.charset()));
                    break;
                case BYTES:
                    setFieldValue(field, instance, readBytes(buf, anno.length()));
                    break;
            }
        }
        return instance;
    }

    private static <T> boolean decodeFieldWithConverter(ByteBuf buf, Field field, MessageField anno, T instance) throws Exception {
        if (anno.converter() != null && !anno.converter().equals(DefaultConverter.class)) {
            FieldConverter fieldConverter = anno.converter().getDeclaredConstructor().newInstance();
            Object decode = fieldConverter.decode(buf, anno);
            field.set(instance, decode);
            return true;
        }
        return false;
    }

    private static void setFieldValue(Field field, Object instance, Object value) throws IllegalAccessException {
        field.set(instance, value);
    }

    private static String readBcd(ByteBuf buf, int length) {
        byte[] bcdBytes = new byte[length];
        buf.readBytes(bcdBytes);
        return BcdUtil.bcdToString(bcdBytes);
    }

    private static String readString(ByteBuf buf, int length, String charset) {
        int len = length > 0 ? length : buf.readableBytes();
        return buf.readCharSequence(len, Charset.forName(charset)).toString();
    }

    private static byte[] readBytes(ByteBuf buf, int length) {
        byte[] bytes = new byte[length];
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
                log.info("使用自定义编码逻辑: {}", msg.getClass().getSimpleName());
                return;
            }
            log.error("自定义编码失败，回退到默认编码逻辑: {}", msg.getClass().getSimpleName());
        }

        Field[] fields = msg.getClass().getDeclaredFields();

        // 按order排序字段，使用Optional避免空指针异常
        Arrays.sort(fields, Comparator.comparingInt(f ->
                Optional.ofNullable(f.getAnnotation(MessageField.class))
                        .map(MessageField::order)
                        .orElse(Integer.MAX_VALUE)));

        for (Field field : fields) {
            MessageField annotation = field.getAnnotation(MessageField.class);
            if (annotation == null) continue;

            field.setAccessible(true);
            Object fieldValue = field.get(msg);

            // 跳过null值字段
            if (fieldValue == null) continue;

            // 优先处理自定义转换器
            if (encodeFieldWithConverter(bodyBuf, annotation, fieldValue)) {
                continue;
            }

            // 处理标准字段类型
            writeFieldToByteBuf(bodyBuf, annotation, fieldValue);
        }
    }

    /**
     * 处理具有自定义转换器的字段
     * @param bodyBuf 目标ByteBuf
     * @param annotation 字段注解
     * @param fieldValue 字段值
     * @return 是否已处理
     */
    private static boolean encodeFieldWithConverter(ByteBuf bodyBuf, MessageField annotation, Object fieldValue) {
        Class<? extends FieldConverter> converterClass = annotation.converter();

        if (converterClass != null && !converterClass.equals(DefaultConverter.class)) {
            try {
                FieldConverter converter = getOrCreateConverter(converterClass);
                converter.encode(bodyBuf, fieldValue, annotation);
                return true;
            } catch (Exception e) {
                throw new RuntimeException("Failed to encode field with converter: " +
                        converterClass.getSimpleName(), e);
            }
        }
        return false;
    }

    /**
     * 根据字段类型将值写入ByteBuf
     * @param bodyBuf 目标ByteBuf
     * @param annotation 字段注解
     * @param fieldValue 字段值
     * @throws IllegalArgumentException 类型转换异常
     */
    private static void writeFieldToByteBuf(ByteBuf bodyBuf, MessageField annotation, Object fieldValue) {
        try {
            switch (annotation.type()) {
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
                    writeBcd(bodyBuf, fieldValue);
                    break;
                case STRING:
                    writeString(bodyBuf, fieldValue, annotation.charset());
                    break;
                case BYTES:
                    writeBytes(bodyBuf, fieldValue);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported field type: " + annotation.type());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to write field of type: " + annotation.type(), e);
        }
    }

    /**
     * 写入字节值
     */
    private static void writeByte(ByteBuf bodyBuf, Object value) {
        if (value instanceof Integer) {
            bodyBuf.writeByte((Integer) value);
        } else if (value instanceof Byte) {
            bodyBuf.writeByte((Byte) value);
        } else {
            throw new IllegalArgumentException("BYTE field must be Integer or Byte type, but got: " +
                    value.getClass().getSimpleName());
        }
    }

    /**
     * 写入短整型值
     */
    private static void writeWord(ByteBuf bodyBuf, Object value) {
        if (value instanceof Integer) {
            bodyBuf.writeShort((Integer) value);
        } else if (value instanceof Short) {
            bodyBuf.writeShort((Short) value);
        } else {
            throw new IllegalArgumentException("WORD field must be Integer or Short type, but got: " +
                    value.getClass().getSimpleName());
        }
    }

    /**
     * 写入双字值
     */
    private static void writeDWord(ByteBuf bodyBuf, Object value) {
        if (value instanceof Integer) {
            bodyBuf.writeInt((Integer) value);
        } else if (value instanceof Long) {
            // 处理长整型，但需要检查范围
            long longValue = (Long) value;
            if (longValue > Integer.MAX_VALUE || longValue < Integer.MIN_VALUE) {
                throw new IllegalArgumentException("DWORD value out of range: " + longValue);
            }
            bodyBuf.writeInt((int) longValue);
        } else {
            throw new IllegalArgumentException("DWORD field must be Integer or Long type, but got: " +
                    value.getClass().getSimpleName());
        }
    }

    /**
     * 写入BCD编码字符串
     */
    private static void writeBcd(ByteBuf bodyBuf, Object value) {
        if (!(value instanceof String bcdString)) {
            throw new IllegalArgumentException("BCD field must be String type, but got: " +
                    value.getClass().getSimpleName());
        }

        try {
            byte[] bcdBytes = BcdUtil.stringToBcd(bcdString);
            bodyBuf.writeBytes(bcdBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode BCD string: " + bcdString, e);
        }
    }

    /**
     * 写入字符串
     */
    private static void writeString(ByteBuf bodyBuf, Object value, String charsetName) {
        if (!(value instanceof String stringValue)) {
            throw new IllegalArgumentException("STRING field must be String type, but got: " +
                    value.getClass().getSimpleName());
        }

        try {
            Charset charset = Charset.forName(charsetName);
            bodyBuf.writeCharSequence(stringValue, charset);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode string with charset: " + charsetName, e);
        }
    }

    /**
     * 写入字节数组
     */
    private static void writeBytes(ByteBuf bodyBuf, Object value) {
        if (!(value instanceof byte[] bytes)) {
            throw new IllegalArgumentException("BYTES field must be byte[] type, but got: " +
                    value.getClass().getSimpleName());
        }

        bodyBuf.writeBytes(bytes);
    }

    /**
     * 获取或创建转换器实例（可以考虑使用缓存优化）
     */
    private static FieldConverter getOrCreateConverter(Class<? extends FieldConverter> converterClass)
            throws Exception {
        return converterClass.getDeclaredConstructor().newInstance();
    }
}