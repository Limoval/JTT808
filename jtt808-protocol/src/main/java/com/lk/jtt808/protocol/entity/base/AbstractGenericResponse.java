package com.lk.jtt808.protocol.entity.base;

import com.lk.jtt808.protocol.annotation.field.ByteField;
import com.lk.jtt808.protocol.annotation.field.WordField;
import com.lk.jtt808.protocol.constant.ResponseCode;
import com.lk.jtt808.protocol.entity.JT808Message;
import com.lk.jtt808.protocol.entity.JT808Response;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通用应答消息基类
 * T0001（终端通用应答）和 T8001（平台通用应答）的公共基类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractGenericResponse extends JT808Message implements JT808Response {

    /** 成功/确认 */
    public static final int SUCCESS = ResponseCode.SUCCESS;
    /** 失败 */
    public static final int FAILURE = ResponseCode.FAILURE;
    /** 消息有误 */
    public static final int MESSAGE_ERROR = ResponseCode.MESSAGE_ERROR;
    /** 不支持 */
    public static final int NOT_SUPPORT = ResponseCode.NOT_SUPPORT;
    /** 报警处理确认 */
    public static final int ALARM_ACK = ResponseCode.ALARM_ACK;

    @WordField
    private int responseSerialNo;

    @WordField
    private int responseMessageId;

    @ByteField
    private int resultCode;

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return this.resultCode == SUCCESS;
    }

    /**
     * 判断是否失败
     */
    public boolean isFailure() {
        return this.resultCode == FAILURE;
    }

    /**
     * 判断是否消息有误
     */
    public boolean isMessageError() {
        return this.resultCode == MESSAGE_ERROR;
    }

    /**
     * 判断是否不支持
     */
    public boolean isNotSupport() {
        return this.resultCode == NOT_SUPPORT;
    }
}
