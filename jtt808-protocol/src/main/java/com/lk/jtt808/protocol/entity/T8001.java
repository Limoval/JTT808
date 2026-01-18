package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageField;
import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.entity.enums.DataType;
import com.lk.jtt808.protocol.constant.JT808;
import lombok.Data;

/**
 * @author: Limoval
 * time: 2025/5/26 15:27 周一
 * description:
 */
@MessageType(JT808.平台通用应答)
@Data
public class T8001 extends JT808Message {

    public static final int Success = 0; //成功、确认
    public static final int Failure = 1;//失败
    public static final int MessageError = 2;//消息有误
    public static final int NotSupport = 3;//不支持
    public static final int AlarmAck = 4;//报警处理确认

    @MessageField(order = 1, type = DataType.WORD, desc = "应答流水号")
    private int responseSerialNo;
    @MessageField(order = 2, type = DataType.WORD, desc = "应答消息ID")
    private int responseMessageId;
    @MessageField(order = 3, type = DataType.BYTE, desc = "结果")
    private int resultCode;

    public boolean isSuccess() {
        return this.resultCode == Success;
    }
}
