package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.annotation.field.ByteField;
import com.lk.jtt808.protocol.annotation.field.BytesField;
import com.lk.jtt808.protocol.annotation.field.StringField;
import com.lk.jtt808.protocol.annotation.field.WordField;
import com.lk.jtt808.protocol.constant.JT808;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 终端注册
 * 消息ID: 0x0100
 */
@MessageType(JT808.终端注册)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class T0100 extends JT808Message {

    @WordField
    private int provinceId;

    @WordField
    private int cityId;

    @BytesField(length = 5)
    private byte[] producerId;

    @BytesField(length = 20)
    private byte[] terminalType;

    @BytesField(length = 7)
    private byte[] terminalId;

    @ByteField
    private int licenseColor;

    @StringField
    private String license;
}
