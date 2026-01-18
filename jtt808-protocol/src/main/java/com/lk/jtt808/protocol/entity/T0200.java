package com.lk.jtt808.protocol.entity;


import com.lk.jtt808.protocol.annotation.MessageField;
import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.converter.AdditionalInfoConverter;
import com.lk.jtt808.protocol.converter.LatLonConverter;
import com.lk.jtt808.protocol.converter.SpeedConverter;
import com.lk.jtt808.protocol.entity.enums.DataType;
import com.lk.jtt808.protocol.constant.JT808;
import lombok.Data;

import java.util.List;

@MessageType(JT808.位置信息汇报)
@Data
public class T0200 extends JT808Message {

    @MessageField(order = 1, type = DataType.DWORD, desc = "报警标志")
    private Long alarmFlag;

    @MessageField(order = 2, type = DataType.DWORD, desc = "状态标志")
    private Long statusFlag;

    @MessageField(order = 3, type = DataType.DWORD, converter = LatLonConverter.class, desc = "纬度")
    private Double latitude;

    @MessageField(order = 4, type = DataType.DWORD, converter = LatLonConverter.class, desc = "经度")
    private Double longitude;

    @MessageField(order = 5, type = DataType.WORD, desc = "高度")
    private int altitude;

    @MessageField(order = 6, type = DataType.WORD, converter = SpeedConverter.class, desc = "速度")
    private int speed;

    @MessageField(order = 7, type = DataType.WORD, desc = "方向")
    private int direction;

    @MessageField(order = 8, type = DataType.BCD, length = 6, desc = "时间")
    private String time;

    @MessageField(order = 9, type = DataType.BYTES, desc = "附加信息", converter = AdditionalInfoConverter.class)
    private List<AdditionalInfo> additionalInfos;

    @Data
    public static class AdditionalInfo {
        private int id;              // 附加信息ID
        private int length;          // 附加信息长度
        private byte[] content;      // 附加信息内容
    }
}
