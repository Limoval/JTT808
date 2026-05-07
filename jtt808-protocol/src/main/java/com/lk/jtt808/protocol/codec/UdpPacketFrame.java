package com.lk.jtt808.protocol.codec;

import io.netty.buffer.ByteBuf;

import java.net.InetSocketAddress;

/**
 * UDP 解码后的协议帧，携带发送方地址。
 */
public record UdpPacketFrame(ByteBuf content, InetSocketAddress sender) {
}
