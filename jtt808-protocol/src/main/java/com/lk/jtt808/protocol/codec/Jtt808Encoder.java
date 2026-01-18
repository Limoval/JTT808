package com.lk.jtt808.protocol.codec;


import com.lk.jtt808.protocol.annotation.MessageConverter;
import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.entity.JT808Message;
import com.lk.jtt808.protocol.util.BcdUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * JTT808协议编码器
 * 支持2013和2019两种协议版本
 */
@Slf4j
public class Jtt808Encoder extends MessageToByteEncoder<JT808Message> {

    /** 2013版本协议 */
    private static final int PROTOCOL_VERSION_2013 = 2013;
    /** 2019版本协议 */
    private static final int PROTOCOL_VERSION_2019 = 2019;

    @Override
    protected void encode(ChannelHandlerContext ctx, JT808Message msg, ByteBuf byteBuf) throws Exception {
        // 1. 构建消息体
        ByteBuf bodyBuf = ctx.alloc().buffer();
        try {
            MessageConverter.toByteBuf(bodyBuf, msg);

            // 2. 构建消息头（根据协议版本选择不同的构建方式）
            ByteBuf headerBuf = buildMessageHeader(ctx, msg, bodyBuf.readableBytes());

            // 3. 合并消息头和消息体
            ByteBuf messageBuf = ctx.alloc().compositeBuffer(2)
                    .addComponent(true, headerBuf)
                    .addComponent(true, bodyBuf);

            // 4. 计算校验码
            byte checkCode = calculateCheckCode(messageBuf);

            // 5. 构建完整报文
            ByteBuf finalBuf = ctx.alloc().buffer();
            try {
                finalBuf.writeBytes(messageBuf);
                finalBuf.writeByte(checkCode);
                escapeData(finalBuf, byteBuf);

                if (log.isDebugEnabled()) {
                    ByteBuf copy = byteBuf.copy();
                    String hexDump = ByteBufUtil.prettyHexDump(copy);
                    log.debug("发送报文：\n{}", hexDump);
                    copy.release();
                }

            } finally {
                finalBuf.release();
            }
        } finally {
            bodyBuf.release();
        }
    }

    /**
     * 构建消息头
     * 根据消息的协议版本选择对应的格式
     */
    private ByteBuf buildMessageHeader(ChannelHandlerContext ctx, JT808Message msg, int bodyLength) {
        int protocolVersion = msg.getProtocolVersion();

        // 默认使用2013版本
        if (protocolVersion == 0) {
            protocolVersion = PROTOCOL_VERSION_2013;
        }

        if (protocolVersion == PROTOCOL_VERSION_2019) {
            return buildMessageHeader2019(ctx, msg, bodyLength);
        } else {
            return buildMessageHeader2013(ctx, msg, bodyLength);
        }
    }

    /**
     * 构建2013版本消息头
     * 消息头结构：消息ID(2) + 消息体属性(2) + 终端手机号(6 BCD) + 流水号(2) = 12字节
     */
    private ByteBuf buildMessageHeader2013(ChannelHandlerContext ctx, JT808Message msg, int bodyLength) {
        ByteBuf headerBuf = ctx.alloc().buffer(12);

        int messageId = getMessageId(msg);

        // 消息ID (2字节)
        headerBuf.writeShort(messageId);

        // 消息体属性 (2字节)：消息体长度10位 + 加密方式2位 + 分包1位 + 保留3位
        int attr = bodyLength & 0x3FF;  // 低10位为消息体长度
        headerBuf.writeShort(attr);

        // 终端手机号 (6字节 BCD码)
        String clientId = String.format("%012d", Long.parseLong(msg.getClientId()));
        byte[] terminalBytes = BcdUtil.stringToBcd(clientId);
        headerBuf.writeBytes(terminalBytes);

        // 流水号 (2字节)
        headerBuf.writeShort(msg.getOutboundSerialNo());

        log.debug("构建2013消息头: msgId=0x{}, bodyLen={}, clientId={}, serialNo={}",
                Integer.toHexString(messageId), bodyLength, clientId, msg.getOutboundSerialNo());

        return headerBuf;
    }

    /**
     * 构建2019版本消息头
     * 消息头结构：消息ID(2) + 消息体属性(2) + 协议版本号(1) + 终端手机号(10 BCD) + 流水号(2) = 17字节
     */
    private ByteBuf buildMessageHeader2019(ChannelHandlerContext ctx, JT808Message msg, int bodyLength) {
        ByteBuf headerBuf = ctx.alloc().buffer(17);

        int messageId = getMessageId(msg);

        // 消息ID (2字节)
        headerBuf.writeShort(messageId);

        // 消息体属性 (2字节)：消息体长度12位 + 加密方式3位 + 分包1位 + 版本标识1位(固定为1)
        int attr = (bodyLength & 0x0FFF) | (1 << 14);  // bit14=1表示2019版本
        headerBuf.writeShort(attr);

        // 协议版本号 (1字节)：固定为1
        headerBuf.writeByte(1);

        // 终端手机号 (10字节 BCD码)
        String clientId = String.format("%020d", new java.math.BigInteger(msg.getClientId()));
        byte[] terminalBytes = BcdUtil.stringToBcd(clientId);
        headerBuf.writeBytes(terminalBytes);

        // 流水号 (2字节)
        headerBuf.writeShort(msg.getOutboundSerialNo());

        log.debug("构建2019消息头: msgId=0x{}, bodyLen={}, clientId={}, serialNo={}",
                Integer.toHexString(messageId), bodyLength, clientId, msg.getOutboundSerialNo());

        return headerBuf;
    }

    /**
     * 获取消息ID
     */
    private int getMessageId(JT808Message msg) {
        int messageId = msg.getMessageId();
        if (messageId == 0) {
            MessageType annotation = msg.getClass().getAnnotation(MessageType.class);
            if (annotation != null) {
                messageId = annotation.value();
            }
        }
        return messageId;
    }

    // 计算校验码
    private byte calculateCheckCode(ByteBuf data) {
        byte checksum = 0;
        data.markReaderIndex();
        while (data.isReadable()) {
            checksum ^= data.readByte();
        }
        data.resetReaderIndex();
        return checksum;
    }

    // 数据转义处理
    private void escapeData(ByteBuf in, ByteBuf out) {
        out.writeByte(0x7E); // 起始符

        while (in.isReadable()) {
            byte b = in.readByte();
            if (b == 0x7E) { // 0x7E -> 0x7D 0x02
                out.writeByte(0x7D);
                out.writeByte(0x02);
            } else if (b == 0x7D) { // 0x7D -> 0x7D 0x01
                out.writeByte(0x7D);
                out.writeByte(0x01);
            } else {
                out.writeByte(b);
            }
        }

        out.writeByte(0x7E); // 结束符
    }


}
