package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.annotation.field.ByteField;
import com.lk.jtt808.protocol.annotation.field.DWordField;
import com.lk.jtt808.protocol.constant.JT808;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 多媒体事件信息上传
 * 消息ID: 0x0800
 */
@MessageType(JT808.多媒体事件信息上传)
@EqualsAndHashCode(callSuper = true)
@Data
public class T0800 extends JT808Message {

    @DWordField(desc = "多媒体数据ID")
    private long mediaId;

    @ByteField(desc = "多媒体类型")
    private int mediaType;

    @ByteField(desc = "多媒体格式编码")
    private int formatCode;

    @ByteField(desc = "事件项编码")
    private int eventCode;

    @ByteField(desc = "通道ID")
    private int channelId;
}
