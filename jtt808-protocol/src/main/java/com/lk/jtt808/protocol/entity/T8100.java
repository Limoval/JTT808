package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.annotation.field.ByteField;
import com.lk.jtt808.protocol.annotation.field.StringField;
import com.lk.jtt808.protocol.annotation.field.WordField;
import com.lk.jtt808.protocol.constant.JT808;
import com.lk.jtt808.protocol.constant.ResponseCode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 终端注册应答
 * 消息ID: 0x8100
 */
@EqualsAndHashCode(callSuper = true)
@Data
@MessageType(JT808.终端注册应答)
@ToString
public class T8100 extends JT808Message {

    /** 0.成功 */
    public static final int Success = ResponseCode.REGISTER_SUCCESS;
    /** 1.车辆已被注册 */
    public static final int AlreadyRegisteredVehicle = ResponseCode.VEHICLE_ALREADY_REGISTERED;
    /** 2.数据库中无该车辆 */
    public static final int NotFoundVehicle = ResponseCode.VEHICLE_NOT_FOUND;
    /** 3.终端已被注册 */
    public static final int AlreadyRegisteredTerminal = ResponseCode.TERMINAL_ALREADY_REGISTERED;
    /** 4.数据库中无该终端 */
    public static final int NotFoundTerminal = ResponseCode.TERMINAL_NOT_FOUND;

    @WordField
    private int responseSerialNo;

    @ByteField
    private int resultCode;

    @StringField(charset = "UTF8")
    private String token;
}
