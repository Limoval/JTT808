package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.annotation.field.StringField;
import com.lk.jtt808.protocol.constant.JT808;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 终端鉴权
 * 消息ID: 0x0102
 */
@MessageType(JT808.终端鉴权)
@EqualsAndHashCode(callSuper = true)
@Data
public class T0102 extends JT808Message {

    @StringField
    private String authCode;
}
