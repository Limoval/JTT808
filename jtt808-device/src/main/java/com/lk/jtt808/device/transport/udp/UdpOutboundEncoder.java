package com.lk.jtt808.device.transport.udp;

import com.lk.jtt808.protocol.codec.Jtt808Encoder;
import com.lk.jtt808.protocol.entity.JT808Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * UDP 出站编码器
 * 将 UdpOutbound 转换为 DatagramPacket
 * <p>
 * 如果 payload 是 JT808Message，会使用嵌入式 Jtt808Encoder 完成协议编码；
 * 如果 payload 已经是 ByteBuf，则直接包装。
 */
@Slf4j
public class UdpOutboundEncoder extends MessageToMessageEncoder<UdpOutbound> {

    @Override
    protected void encode(ChannelHandlerContext ctx, UdpOutbound msg, List<Object> out) {
        Object payload = msg.message();
        ByteBuf encodedBuf = null;

        if (payload instanceof ByteBuf buf) {
            encodedBuf = buf.retainedDuplicate();
        } else if (payload instanceof JT808Message jtMsg) {
            // 使用嵌入式 channel 完成 JT808 协议编码
            EmbeddedChannel embedded = new EmbeddedChannel(new Jtt808Encoder());
            try {
                embedded.writeOutbound(jtMsg);
                encodedBuf = embedded.readOutbound();
            } finally {
                embedded.finish();
            }
            if (encodedBuf == null) {
                log.error("UDP 出站消息编码失败: {}", jtMsg);
                return;
            }
        } else {
            log.warn("UdpOutbound payload 类型不支持: {}", payload != null ? payload.getClass().getName() : "null");
            return;
        }

        out.add(new DatagramPacket(encodedBuf, msg.recipient()));
    }
}
