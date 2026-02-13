package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.annotation.field.BcdField;
import com.lk.jtt808.protocol.annotation.field.BytesField;
import com.lk.jtt808.protocol.annotation.field.DWordField;
import com.lk.jtt808.protocol.annotation.field.WordField;
import com.lk.jtt808.protocol.constant.JT808;
import com.lk.jtt808.protocol.converter.AdditionalInfoConverter;
import com.lk.jtt808.protocol.converter.LatLonConverter;
import com.lk.jtt808.protocol.converter.SpeedConverter;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 位置信息汇报
 * 消息ID: 0x0200
 */
@MessageType(JT808.位置信息汇报)
@EqualsAndHashCode(callSuper = true)
@Data
public class T0200 extends JT808Message {

    @DWordField
    private Long alarmFlag;

    @DWordField
    private Long statusFlag;

    @DWordField(converter = LatLonConverter.class)
    private Double latitude;

    @DWordField(converter = LatLonConverter.class)
    private Double longitude;

    @WordField
    private int altitude;

    @WordField(converter = SpeedConverter.class)
    private int speed;

    @WordField
    private int direction;

    @BcdField(length = 6)
    private String time;

    @BytesField(converter = AdditionalInfoConverter.class)
    private List<AdditionalInfo> additionalInfos;

    @Data
    public static class AdditionalInfo {
        private int id;              // 附加信息ID
        private int length;          // 附加信息长度
        private byte[] content;      // 附加信息内容
    }
}
