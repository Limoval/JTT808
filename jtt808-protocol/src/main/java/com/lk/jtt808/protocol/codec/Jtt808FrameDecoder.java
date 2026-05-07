package com.lk.jtt808.protocol.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class Jtt808FrameDecoder extends ByteToMessageDecoder {

    private static final byte DELIMITER = 0x7E;
    private static final byte ESCAPE = 0x7D;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {

        if (log.isDebugEnabled()) {
            String hexDump = ByteBufUtil.hexDump(in, in.readerIndex(), in.readableBytes());
            log.debug("收到报文：{}", hexDump);
        }

        int startIndex = in.indexOf(in.readerIndex(), in.writerIndex(), DELIMITER);
        if (startIndex == -1) return;

        int endIndex = in.indexOf(startIndex + 1, in.writerIndex(), DELIMITER);
        if (endIndex == -1) return;

        // 提取帧数据（包含起始和结束标志）
        ByteBuf frame = in.retainedSlice(startIndex, endIndex - startIndex + 1);
        in.readerIndex(endIndex + 1);

        // 反转义
        ByteBuf unescaped = unescape(frame);
        frame.release();

        // 校验
        if (verify(unescaped)) {
            out.add(unescaped);
        } else {
            unescaped.release();
        }
    }

    /**
     * 反转义处理
     *
     * @param source 原始ByteBuf
     * @return 反转义后的ByteBuf
     */
    private ByteBuf unescape(ByteBuf source) {
        int readerIndex = source.readerIndex();
        int writerIndex = source.writerIndex();

        // 检查并跳过起始标识符0x7e
        if (source.getByte(readerIndex) == DELIMITER) {
            readerIndex++;
        }

        // 检查并排除结束标识符0x7e
        if (source.getByte(writerIndex - 1) == DELIMITER) {
            writerIndex--;
        }

        // 创建结果ByteBuf
        ByteBuf result = Unpooled.buffer(writerIndex - readerIndex);

        // 处理转义字符
        for (int i = readerIndex; i < writerIndex; i++) {
            byte b = source.getByte(i);
            if (b == ESCAPE) {
                i++;
                if (i < writerIndex) {
                    byte nextByte = source.getByte(i);
                    if (nextByte == 0x01) {
                        result.writeByte(ESCAPE); // 0x7d 0x01 -> 0x7d
                    } else if (nextByte == 0x02) {
                        result.writeByte(DELIMITER); // 0x7d 0x02 -> 0x7e
                    } else {
                        // 错误的转义序列，保持原样
                        result.writeByte(ESCAPE);
                        result.writeByte(nextByte);
                    }
                } else {
                    // 0x7d出现在最后一个字节，直接写入
                    result.writeByte(ESCAPE);
                }
            } else {
                result.writeByte(b);
            }
        }

        return result;
    }

    /**
     * 校验报文
     *
     * @param buf 待校验的ByteBuf
     * @return 校验是否通过
     */
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