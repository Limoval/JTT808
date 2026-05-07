package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.constant.JT808;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 终端注销
 * 消息ID: 0x0003
 */
@MessageType(JT808.终端注销)
@EqualsAndHashCode(callSuper = true)
@Data
public class T0003 extends JT808Message {
}
