package com.lk.jtt808.protocol.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * UDP 帧解码器
 * 接收 DatagramPacket，直接做反转义+校验（UDP 无需流式分帧）
 * 并将发送者地址存入 Channel Attribute
 */
@Slf4j
public class UdpFrameDecoder extends MessageToMessageDecoder<DatagramPacket> {

    private static final byte DELIMITER = 0x7E;
    private static final byte ESCAPE = 0x7D;
    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) {
        ByteBuf content = packet.content();
        InetSocketAddress sender = packet.sender();

        if (log.isDebugEnabled()) {
            log.debug("UDP收到报文: sender={}, hex={}", sender, ByteBufUtil.hexDump(content));
        }

        if (content.readableBytes() < 2) {
            log.warn("UDP报文太短，忽略: sender={}", sender);
            return;
        }

        // 去除首尾 0x7E 标识符
        int start = content.readerIndex();
        int end = content.writerIndex();

        if (content.getByte(start) == DELIMITER) {
            start++;
        }
        if (content.getByte(end - 1) == DELIMITER) {
            end--;
        }

        if (end <= start) {
            log.warn("UDP报文内容为空，忽略: sender={}", sender);
            return;
        }

        // 反转义
        ByteBuf unescaped = unescape(content, start, end);

        // 校验
        if (verify(unescaped)) {
            out.add(new UdpPacketFrame(unescaped, sender));
        } else {
            log.warn("UDP报文校验失败: sender={}", sender);
            unescaped.release();
        }
    }

    private ByteBuf unescape(ByteBuf source, int start, int end) {
        ByteBuf result = Unpooled.buffer(end - start);

        for (int i = start; i < end; i++) {
            byte b = source.getByte(i);
            if (b == ESCAPE && i + 1 < end) {
                byte next = source.getByte(i + 1);
                if (next == 0x01) {
                    result.writeByte(ESCAPE);
                    i++;
                } else if (next == 0x02) {
                    result.writeByte(DELIMITER);
                    i++;
                } else {
                    result.writeByte(b);
                }
            } else {
                result.writeByte(b);
            }
        }

        return result;
    }

    private boolean verify(ByteBuf buf) {
        if (buf.readableBytes() < 1) return false;

        int length = buf.readableBytes();
        byte checkSum = 0;
        for (int i = 0; i < length - 1; i++) {
            checkSum ^= buf.getByte(i);
        }
        return checkSum == buf.getByte(length - 1);
    }
}
