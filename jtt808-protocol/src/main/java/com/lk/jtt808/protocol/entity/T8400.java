package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.annotation.field.ByteField;
import com.lk.jtt808.protocol.annotation.field.StringField;
import com.lk.jtt808.protocol.constant.JT808;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 电话回拨
 * 消息ID: 0x8400
 */
@MessageType(JT808.电话回拨)
@EqualsAndHashCode(callSuper = true)
@Data
public class T8400 extends JT808Message {

    @ByteField(desc = "标志")
    private int flag;

    @StringField(desc = "电话号码")
    private String phoneNumber;
}
