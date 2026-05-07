package com.lk.jtt808.protocol.codec;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.lk.jtt808.protocol.entity.JT808Message;
import com.lk.jtt808.protocol.util.BcdUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Jtt808MessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    /** 分包消息缓存（带TTL和容量限制，防止内存泄漏） */
    private final Cache<String, Map<Integer, byte[]>> packageCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10000)
            .removalListener((String key, Map<Integer, byte[]> value, RemovalCause cause) -> {
                if (cause == RemovalCause.EXPIRED) {
                    log.warn("分包数据过期被清理: key={}", key);
                } else if (cause == RemovalCause.SIZE) {
                    log.warn("分包缓存容量已满，清理最旧数据: key={}", key);
                }
            })
            .build();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        // 复制一份ByteBuf以便解码过程中不影响原始数据
        ByteBuf in = msg.copy();
        try {
            // 解码JT808消息
            JT808Message message = decode(in);
            if (log.isDebugEnabled()) {
                log.debug("Decoded JT808 message: {}", message);
            }

            // 如果解码成功且消息有效，添加到输出列表
            if (message != null) {
                out.add(message);
            }
        } finally {
            if (in.refCnt() > 0) {
                in.release();
            }
        }
    }



    public JT808Message decode(ByteBuf input) {
        JT808Message message = new JT808Message();
        try {
            if (input.readableBytes() < 2) {
                throw new IllegalArgumentException("Message too short: missing messageId");
            }
            int messageId = input.readUnsignedShort();
            message.setMessageId(messageId);
            message.setVerified(true);

            // 解析消息头
            parseHeader(input, message);

            // 解析消息体
            if (message.isSubpackage()) {
                // 处理分包消息
                boolean complete = handleSubpackage(input, message);
                if (!complete) {
                    // 分包未完整，不返回消息
                    return null;
                }
            } else {
                // 处理普通消息
                parseBody(input, message);
            }

            return message;
        } catch (Exception e) {
            log.error("Error decoding JT808 message: {}", e.getMessage(), e);
            return null;
        }
    }


    /**
     * 解析消息头
     *
     * @param buf ByteBuf数据
     * @param message 消息对象
     */
    private void parseHeader(ByteBuf buf, JT808Message message) {
        // 保存初始读取位置（假设已经读取了消息ID）
        int initialPosition = buf.readerIndex();
        if (buf.readableBytes() < 2) {
            throw new IllegalArgumentException("Message too short: missing properties, messageId=0x"
                    + Integer.toHexString(message.getMessageId()));
        }

        // 解析消息体属性（16位）
        int properties = buf.getUnsignedShort(initialPosition);

        // 统一位域解析（2013/2019 兼容）
        int bodyLength = properties & 0x3FF;           // bit0-9: 消息体长度（10位）
        int encryptionType = (properties >> 10) & 0x07; // bit10-12: 加密方式（3位）
        boolean subpackage = ((properties >> 13) & 0x01) == 1; // bit13: 分包标志
        boolean versionFlag = ((properties >> 14) & 0x01) == 1; // bit14: 版本标识（2013=0, 2019=1）

        message.setBodyLength(bodyLength);
        message.setEncryptionType(encryptionType);
        message.setSubpackage(subpackage);

        int headerLengthAfterMessageId = 2
                + (versionFlag ? 1 + 10 + 2 : 6 + 2)
                + (subpackage ? 4 : 0);
        int requiredLengthAfterMessageId = headerLengthAfterMessageId + bodyLength + 1;
        if (buf.readableBytes() < requiredLengthAfterMessageId) {
            throw new IllegalArgumentException("Message too short: messageId=0x"
                    + Integer.toHexString(message.getMessageId())
                    + ", declaredBodyLength=" + bodyLength
                    + ", requiredAfterMessageId=" + requiredLengthAfterMessageId
                    + ", actualAfterMessageId=" + buf.readableBytes());
        }

        // 移动读取指针到属性后
        buf.readerIndex(initialPosition + 2);

        if (versionFlag) {
            // 2019 版本协议
            message.setProtocolVersion(2019);
            // 读取协议版本号（1字节，通常为 0x01）
            message.setProtocolVersionByte(buf.readUnsignedByte());
            // 读取终端手机号（10字节BCD码）
            byte[] phoneBytes = new byte[10];
            buf.readBytes(phoneBytes);
            String clientId = BcdUtil.bcdToString(phoneBytes);
            message.setClientId(clientId);
        } else {
            // 2013 版本协议
            message.setProtocolVersion(2013);
            message.setProtocolVersionByte(0);
            // 读取终端手机号（6字节BCD码）
            byte[] phoneBytes = new byte[6];
            buf.readBytes(phoneBytes);
            String clientId = BcdUtil.bcdToString(phoneBytes);
            message.setClientId(clientId);
        }

        // 消息流水号（2字节）
        message.setInboundSerialNo(buf.readUnsignedShort());

        // 分包处理
        if (subpackage) {
            message.setTotalPackage(buf.readUnsignedShort());
            message.setPackageIndex(buf.readUnsignedShort());
        }
    }


    /**
     * 解析消息体
     *
     * @param buf ByteBuf数据
     * @param message 消息对象
     */
    private void parseBody(ByteBuf buf, JT808Message message) {
        int bodyLength = message.getBodyLength();
        validateBodyLength(buf, message);
        if (bodyLength > 0) {
            byte[] bodyData = new byte[bodyLength];
            buf.readBytes(bodyData);
            message.setMessageBody(bodyData);
        } else {
            message.setMessageBody(new byte[0]);
        }
    }

    /**
     * 处理分包消息
     *
     * @param buf ByteBuf数据
     * @param message 消息对象
     * @return true 表示所有分包已收齐并合并完成；false 表示还需等待后续分包
     */
    private boolean handleSubpackage(ByteBuf buf, JT808Message message) {
        if (message.getTotalPackage() <= 0
                || message.getPackageIndex() <= 0
                || message.getPackageIndex() > message.getTotalPackage()) {
            throw new IllegalArgumentException("Invalid subpackage header: total="
                    + message.getTotalPackage() + ", index=" + message.getPackageIndex());
        }

        int bodyLength = message.getBodyLength();
        validateBodyLength(buf, message);
        byte[] bodyData = new byte[bodyLength];
        if (bodyLength > 0) {
            buf.readBytes(bodyData);
        }

        // 缓存分包
        String key = message.getClientId() + "_" + message.getInboundSerialNo();
        Map<Integer, byte[]> packageMap = packageCache.get(key, k -> new HashMap<>());
        packageMap.put(message.getPackageIndex(), bodyData);

        // 检查是否所有分包都已接收
        if (packageMap.size() == message.getTotalPackage()) {
            // 合并所有分包
            List<byte[]> packages = new ArrayList<>();
            int totalLength = 0;

            for (int i = 1; i <= message.getTotalPackage(); i++) {
                byte[] pack = packageMap.get(i);
                if (pack != null) {
                    packages.add(pack);
                    totalLength += pack.length;
                } else {
                    log.warn("分包缺失: key={}, missing packageIndex={}", key, i);
                    return false;
                }
            }

            // 合并分包数据
            byte[] completeBody = new byte[totalLength];
            int destPos = 0;

            for (byte[] pack : packages) {
                System.arraycopy(pack, 0, completeBody, destPos, pack.length);
                destPos += pack.length;
            }

            message.setMessageBody(completeBody);

            // 清理缓存
            packageCache.invalidate(key);
            return true;
        } else {
            // 还未收到所有分包，不设置消息体，返回 false 表示未完整
            if (log.isDebugEnabled()) {
                log.debug("分包等待中: key={}, received={}/{}", key, packageMap.size(), message.getTotalPackage());
            }
            return false;
        }
    }

    private void validateBodyLength(ByteBuf buf, JT808Message message) {
        int expectedLength = message.getBodyLength();
        int readableWithoutChecksum = buf.readableBytes() - 1;
        if (readableWithoutChecksum < 0) {
            throw new IllegalArgumentException("Missing checksum byte: messageId=0x"
                    + Integer.toHexString(message.getMessageId()));
        }
        if (readableWithoutChecksum != expectedLength) {
            throw new IllegalArgumentException("Message body length mismatch: messageId=0x"
                    + Integer.toHexString(message.getMessageId())
                    + ", declared=" + expectedLength
                    + ", actual=" + readableWithoutChecksum);
        }
    }

    /**
     * 清理分包缓存
     * 可以定期调用此方法，清理过期的分包数据
     */
    public void clearPackageCache() {
        packageCache.invalidateAll();
    }

    /**
     * 获取分包缓存统计信息
     */
    public long getPackageCacheSize() {
        return packageCache.estimatedSize();
    }
}
