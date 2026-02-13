package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.constant.JT808;
import com.lk.jtt808.protocol.entity.base.AbstractGenericResponse;

/**
 * 平台通用应答
 * 消息ID: 0x8001
 */
@MessageType(JT808.平台通用应答)
public class T8001 extends AbstractGenericResponse {
    // 所有字段和方法都从 AbstractGenericResponse 继承
    // 保留静态常量别名以保持向后兼容
    public static final int Success = SUCCESS;
    public static final int Failure = FAILURE;
    public static final int MessageError = MESSAGE_ERROR;
    public static final int NotSupport = NOT_SUPPORT;
    public static final int AlarmAck = ALARM_ACK;
}
