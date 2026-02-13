package com.lk.jtt808.device.transport.udp;

import java.net.InetSocketAddress;

/**
 * UDP 出站消息包装
 * 将消息与目标地址绑定，用于 Jtt808Encoder 后的 DatagramPacket 封装
 */
public record UdpOutbound(Object message, InetSocketAddress recipient) {
}
