package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.annotation.field.ByteField;
import com.lk.jtt808.protocol.annotation.field.BytesField;
import com.lk.jtt808.protocol.constant.JT808;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据下行透传
 * 消息ID: 0x8900
 */
@MessageType(JT808.数据下行透传)
@EqualsAndHashCode(callSuper = true)
@Data
public class T8900 extends JT808Message {

    @ByteField(desc = "透传消息类型")
    private int passthroughType;

    @BytesField(desc = "透传消息内容")
    private byte[] passthroughContent;
}
