package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.annotation.field.ByteField;
import com.lk.jtt808.protocol.annotation.field.StringField;
import com.lk.jtt808.protocol.constant.JT808;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文本信息下发
 * 消息ID: 0x8300
 */
@MessageType(JT808.文本信息下发)
@EqualsAndHashCode(callSuper = true)
@Data
public class T8300 extends JT808Message {

    @ByteField(desc = "标志")
    private int flag;

    @StringField(desc = "文本信息")
    private String text;
}
