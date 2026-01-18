package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageField;
import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.entity.enums.DataType;
import com.lk.jtt808.protocol.constant.JT808;
import lombok.Data;
import lombok.ToString;

/**
 * @author: Limoval
 * time: 2025/5/21 16:32 周三
 * description:
 */
@MessageType(JT808.终端注册)
@ToString(callSuper = true)
@Data
public class T0100 extends JT808Message {

    @MessageField(order = 1, type = DataType.WORD, desc = "省域ID")
    private int provinceId;

    @MessageField(order = 2, type = DataType.WORD, desc = "市县域ID")
    private int cityId;

    @MessageField(order = 3, type = DataType.BYTES, length = 5, desc = "制造商ID")
    private byte[] producerId;

    @MessageField(order = 4, type = DataType.BYTES, length = 20, desc = "终端型号")
    private byte[] terminalType;

    @MessageField(order = 5, type = DataType.BYTES, length = 7, desc = "终端ID")
    private byte[] terminalId;

    @MessageField(order = 6, type = DataType.BYTE, desc = "车牌颜色")
    private int licenseColor;

    @MessageField(order = 7, type = DataType.STRING, desc = "车牌号")
    private String license;

}
