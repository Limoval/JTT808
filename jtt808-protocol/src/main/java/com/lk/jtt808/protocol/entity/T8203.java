package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.annotation.field.DWordField;
import com.lk.jtt808.protocol.annotation.field.WordField;
import com.lk.jtt808.protocol.constant.JT808;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 人工确认报警消息
 * 消息ID: 0x8203
 */
@MessageType(JT808.人工确认报警消息)
@EqualsAndHashCode(callSuper = true)
@Data
public class T8203 extends JT808Message {

    @WordField(desc = "报警消息流水号")
    private int alarmSerialNo;

    @DWordField(desc = "人工确认报警类型")
    private long alarmType;
}
