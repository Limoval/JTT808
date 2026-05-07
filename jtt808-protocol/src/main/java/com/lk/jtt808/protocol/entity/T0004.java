package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.constant.JT808;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询服务器时间
 * 消息ID: 0x0004
 */
@MessageType(JT808.查询服务器时间)
@EqualsAndHashCode(callSuper = true)
@Data
public class T0004 extends JT808Message {
}
