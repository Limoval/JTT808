package com.lk.jtt808.protocol.codec;


import com.lk.jtt808.protocol.annotation.MessageConverter;
import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.entity.JT808Message;
import com.lk.jtt808.utils.BcdUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: Limoval
 * time: 2025/5/23 9:36 周五
 * description: JTT808协议编码器
 */
@Slf4j
public class Jtt808Encoder extends MessageToByteEncoder<JT808Message> {
    @Override
    protected void encode(ChannelHandlerContext ctx, JT808Message msg, ByteBuf byteBuf) throws Exception {
        // 1. 构建消息
        ByteBuf bodyBuf = ctx.alloc().buffer();
        try {
            MessageConverter.toByteBuf(bodyBuf, msg);

            // 2. 构建消息头
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

                ByteBuf copy = byteBuf.copy();
                String hexDump = ByteBufUtil.prettyHexDump(copy);
                log.info("发送报文：{}", hexDump);

            } finally {
                finalBuf.release();
            }
        } finally {
            bodyBuf.release();
        }
    }


    private ByteBuf buildMessageHeader(ChannelHandlerContext ctx, JT808Message msg, int bodyLength) {
        ByteBuf headerBuf = ctx.alloc().buffer(16);

        int messageId = msg.getMessageId();
        if (messageId == 0) {
            MessageType annotation = msg.getClass().getAnnotation(MessageType.class);
            messageId = annotation.value();
        }

        // 消息ID
        headerBuf.writeShort(messageId);

        // 消息体属性（假设不分包）
        int attr = bodyLength & 0x3FF;
        headerBuf.writeShort(attr);

        // 终端标识
        String clientId = String.format("%012d", Long.parseLong(msg.getClientId()));
        byte[] terminalBytes = BcdUtil.stringToBcd(clientId);
        headerBuf.writeBytes(terminalBytes);

        // 流水号
        int serialNumber = msg.getOutboundSerialNo();
        headerBuf.writeShort(serialNumber);

        log.info("respHeader 响应消息头信息如下 消息ID：{}，消息体属性：{}，终端标识：{}，流水号：{}", messageId, attr, clientId, serialNumber);

        return headerBuf;
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
