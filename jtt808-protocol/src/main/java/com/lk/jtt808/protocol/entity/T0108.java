package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.annotation.field.ByteField;
import com.lk.jtt808.protocol.constant.JT808;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 终端升级结果通知
 * 消息ID: 0x0108
 */
@MessageType(JT808.终端升级结果通知)
@EqualsAndHashCode(callSuper = true)
@Data
public class T0108 extends JT808Message {

    @ByteField(desc = "升级类型")
    private int upgradeType;

    @ByteField(desc = "升级结果")
    private int upgradeResult;
}
