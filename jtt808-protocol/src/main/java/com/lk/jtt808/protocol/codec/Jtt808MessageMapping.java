package com.lk.jtt808.protocol.codec;


import com.lk.jtt808.protocol.annotation.MessageConverter;
import com.lk.jtt808.protocol.annotation.MessageHandlerRegistry;
import com.lk.jtt808.protocol.entity.JT808Message;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@ChannelHandler.Sharable
@Slf4j
public class Jtt808MessageMapping extends MessageToMessageDecoder<JT808Message> {

    @Override
    protected void decode(ChannelHandlerContext ctx, JT808Message jtMsg, List<Object> out) {
        try {
            // 获取注册的消息类型
            Class<? extends JT808Message> messageClass = MessageHandlerRegistry.getMessageClass(jtMsg.getMessageId());

            if (messageClass == null) {
                log.warn("Unsupported message type: 0x{}", 
                    Integer.toHexString(jtMsg.getMessageId()));
                return;
            }

            // 实例化消息对象
            JT808Message message;
            ByteBuf bodyBuf = Unpooled.wrappedBuffer(jtMsg.getMessageBody());
            try {
                message = MessageConverter.parse(bodyBuf, messageClass);
                message.setMessageId(jtMsg.getMessageId());
                message.setClientId(jtMsg.getClientId());
                message.setProtocolVersion(jtMsg.getProtocolVersion());
                message.setProtocolVersionByte(jtMsg.getProtocolVersionByte());
                message.setBodyLength(jtMsg.getBodyLength());
                message.setEncryptionType(jtMsg.getEncryptionType());
                message.setInboundSerialNo(jtMsg.getInboundSerialNo());
                message.setSubpackage(jtMsg.isSubpackage());
                message.setTotalPackage(jtMsg.getTotalPackage());
                message.setPackageIndex(jtMsg.getPackageIndex());
                message.setMessageBody(jtMsg.getMessageBody());
                message.setVerified(jtMsg.isVerified());
                message.setRemoteAddress(jtMsg.getRemoteAddress());
                log.debug("消息映射结果:{}", message);
            } finally {
                bodyBuf.release();
            }
            
            out.add(message);
        } catch (Exception e) {
            log.error("Message mapping failed", e);
            ctx.fireExceptionCaught(e);
        }
    }
}
