package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.annotation.field.DWordField;
import com.lk.jtt808.protocol.annotation.field.WordField;
import com.lk.jtt808.protocol.constant.JT808;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 临时位置跟踪控制
 * 消息ID: 0x8202
 */
@MessageType(JT808.临时位置跟踪控制)
@EqualsAndHashCode(callSuper = true)
@Data
public class T8202 extends JT808Message {

    @WordField(desc = "时间间隔，单位秒")
    private int interval;

    @DWordField(desc = "位置跟踪有效期，单位秒")
    private long validityPeriod;
}
