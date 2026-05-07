package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.constant.JT808;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询终端参数
 * 消息ID: 0x8104
 */
@MessageType(JT808.查询终端参数)
@EqualsAndHashCode(callSuper = true)
@Data
public class T8104 extends JT808Message {
}
