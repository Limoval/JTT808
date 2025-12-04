package com.lk.jtt808.protocol.entity;


import com.lk.jtt808.protocol.annotation.MessageField;
import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.entity.enums.DataType;
import com.lk.jtt808.utils.JT808;
import lombok.Data;
import lombok.ToString;

/**
 * @author: Limoval
 * time: 2025/5/23 14:31 周五
 * description:
 */
@Data
@MessageType(JT808.终端注册应答)
@ToString
public class T8100 extends JT808Message {
    /** 0.成功 */
    public static final int Success = 0;
    /** 1.车辆已被注册 */
    public static final int AlreadyRegisteredVehicle = 1;
    /** 2.数据库中无该车辆 */
    public static final int NotFoundVehicle = 2;
    /** 3.终端已被注册 */
    public static final int AlreadyRegisteredTerminal = 3;
    /** 4.数据库中无该终端 */
    public static final int NotFoundTerminal = 4;

    @MessageField(order = 1, type = DataType.WORD, desc = "应答流水号")
    private int responseSerialNo;
    @MessageField(order = 2, type = DataType.BYTE, desc = "结果：0.成功 1.车辆已被注册 2.数据库中无该车辆 3.终端已被注册 4.数据库中无该终端")
    private int resultCode;
    @MessageField(order = 3, type = DataType.STRING, charset = "UTF8", desc = "鉴权码(成功后才有该字段)")
    private String token;

}
