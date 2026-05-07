package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.annotation.field.ByteField;
import com.lk.jtt808.protocol.annotation.field.StringField;
import com.lk.jtt808.protocol.constant.JT808;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 终端控制
 * 消息ID: 0x8105
 */
@MessageType(JT808.终端控制)
@EqualsAndHashCode(callSuper = true)
@Data
public class T8105 extends JT808Message {

    @ByteField(desc = "命令字")
    private int commandWord;

    @StringField(desc = "命令参数")
    private String commandParam;
}
