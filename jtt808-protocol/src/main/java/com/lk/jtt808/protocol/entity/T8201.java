package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.constant.JT808;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 位置信息查询
 * 消息ID: 0x8201
 */
@MessageType(JT808.位置信息查询)
@EqualsAndHashCode(callSuper = true)
@Data
public class T8201 extends JT808Message {
}
