package com.lk.jtt808.protocol.codec;


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

@Slf4j
public class Jtt808MessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    // 用于存储分包消息的缓存
    private final Map<String, Map<Integer, byte[]>> packageCache = new HashMap<>();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        // 复制一份ByteBuf以便解码过程中不影响原始数据
        ByteBuf in = msg.copy();
        try {
            // 解码JT808消息
            JT808Message message = decode(in);
            log.info("Decoded JT808 message: {}", message);

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
        int i = input.readUnsignedShort();
        message.setMessageId(i);
        message.setVerified(true);


        try {
            // 4. 解析消息头
            parseHeader(input, message);

            // 5. 解析消息体
            if (message.isSubpackage()) {
                // 处理分包消息
                handleSubpackage(input, message);
            } else {
                // 处理普通消息
                parseBody(input, message);
            }

            return message;
        } catch (Exception e) {
            System.err.println("Error decoding JT808 message: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            // 释放反转义后的ByteBuf
            input.release();
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

        // 解析消息体属性（绝对读取，不移动指针）
        int properties = buf.getUnsignedShort(initialPosition);
        boolean hasVersion = ((properties >> 14) & 0x01) == 0x01;
        boolean hasSubPackage = ((properties >> 13) & 0x01) == 0x01;

        // 设置读取指针到属性后
        buf.readerIndex(initialPosition + 2);

        // 根据版本标志处理不同协议版本
        if (hasVersion) {
            // 2019版本协议

            message.setProtocolVersion(2019);
            int bodyLength = properties & 0x0FFF; // 消息体长度，12位
            message.setBodyLength(bodyLength);
            int encryptionType = (properties >> 10) & 0x07; // 加密方式，3位
            message.setEncryptionType(encryptionType);

            byte b = buf.readByte();
            // 读取终端ID（10字节BCD码）
            byte[] phoneBytes = new byte[10];
            buf.readBytes(phoneBytes);
            String clientId = BcdUtil.bcdToString(phoneBytes).replaceFirst("^0+", "");
            message.setClientId(clientId);
        } else {
            // 2013版本协议
            message.setProtocolVersion(2013);
            int bodyLength = properties & 0x3FF; // 消息体长度，10位
            message.setBodyLength(bodyLength);
            int encryptionType = (properties >> 10) & 0x03; // 加密方式，2位
            message.setEncryptionType(encryptionType);

            // 读取终端手机号（6字节BCD码）
            byte[] phoneBytes = new byte[6];
            buf.readBytes(phoneBytes);
            String clientId = BcdUtil.bcdToString(phoneBytes).replaceFirst("^0+", "");
            message.setClientId(clientId);
        }

        // 消息流水号（2字节）
        message.setInboundSerialNo(buf.readUnsignedShort());

        // 分包处理
        if (hasSubPackage) {
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
        int bodyLength = buf.readableBytes() - 1; // 减去校验码1字节
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
     */
    private void handleSubpackage(ByteBuf buf, JT808Message message) {
        // 读取分包消息体
        int bodyLength = buf.readableBytes() - 1; // 减去校验码1字节
        if (bodyLength <= 0) {
            message.setMessageBody(new byte[0]);
            return;
        }

        byte[] bodyData = new byte[bodyLength];
        buf.readBytes(bodyData);

        // 缓存分包
        String key = message.getClientId() + "_" + message.getInboundSerialNo();
        Map<Integer, byte[]> packageMap = packageCache.computeIfAbsent(key, k -> new HashMap<>());
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
            packageCache.remove(key);
        } else {
            // 还未收到所有分包，设置空消息体
            message.setMessageBody(null);
        }
    }

    /**
     * 清理分包缓存
     * 可以定期调用此方法，清理过期的分包数据
     */
    public void clearPackageCache() {
        packageCache.clear();
    }
}