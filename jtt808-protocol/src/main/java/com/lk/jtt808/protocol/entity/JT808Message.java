package com.lk.jtt808.protocol.entity;

import lombok.Data;

@Data
public class JT808Message {
    private int messageId;          // 消息ID
    private int protocolVersion;    // 协议版本号
    private String clientId;     // 终端手机号
    private int bodyLength;
    private int encryptionType;
    private int inboundSerialNo;       // 入站流水号
    private boolean isSubpackage;   // 是否分包
    private int totalPackage;       // 总包数
    private int packageIndex;       // 包序号
    private byte[] messageBody;     // 消息体
    private boolean verified;       // 校验是否通过
    private int outboundSerialNo;       // 出站流水号

}