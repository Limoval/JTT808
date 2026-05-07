package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.annotation.field.ByteField;
import com.lk.jtt808.protocol.constant.JT808;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 车辆控制
 * 消息ID: 0x8500
 */
@MessageType(JT808.车辆控制)
@EqualsAndHashCode(callSuper = true)
@Data
public class T8500 extends JT808Message {

    @ByteField(desc = "控制标志")
    private int controlFlag;
}
