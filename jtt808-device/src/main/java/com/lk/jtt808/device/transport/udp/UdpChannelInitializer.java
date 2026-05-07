package com.lk.jtt808.device.transport.udp;

import com.lk.jtt808.protocol.codec.Jtt808MessageDecoder;
import com.lk.jtt808.protocol.codec.Jtt808MessageMapping;
import com.lk.jtt808.protocol.codec.UdpFrameDecoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioDatagramChannel;

/**
 * UDP Channel 初始化器
 * 配置 UDP 连接的 Netty Pipeline
 */
public class UdpChannelInitializer extends ChannelInitializer<NioDatagramChannel> {

    private final UdpServerHandler udpServerHandler;

    public UdpChannelInitializer(UdpServerHandler udpServerHandler) {
        this.udpServerHandler = udpServerHandler;
    }

    @Override
    protected void initChannel(NioDatagramChannel ch) {
        ch.pipeline()
                // Inbound: UDP 帧解码：DatagramPacket → ByteBuf（反转义+校验）
                .addLast(new UdpFrameDecoder())
                // Inbound: 消息解码：ByteBuf → JT808Message（解析消息头体）
                .addLast(new Jtt808MessageDecoder())
                // Inbound: 消息映射：根据消息ID映射到具体消息类
                .addLast(new Jtt808MessageMapping())
                // Inbound: UDP 业务处理器
                .addLast(udpServerHandler)
                // Outbound: UDP 出站包装器：UdpOutbound → DatagramPacket（内部包含 JT808 编码）
                .addLast(new UdpOutboundEncoder());
    }
}
