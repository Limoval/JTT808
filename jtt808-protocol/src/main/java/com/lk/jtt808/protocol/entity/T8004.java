package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.annotation.field.BcdField;
import com.lk.jtt808.protocol.constant.JT808;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询服务器时间应答
 * 消息ID: 0x8004
 */
@MessageType(JT808.查询服务器时间应答)
@EqualsAndHashCode(callSuper = true)
@Data
public class T8004 extends JT808Message {

    @BcdField(length = 6, desc = "服务器时间 yyMMddHHmmss")
    private String serverTime;
}
