package com.lk.jtt808.protocol.constant;

/**
 * JT808协议响应码常量
 */
public interface ResponseCode {

    // ===== 通用应答结果（T0001/T8001） =====
    /** 成功/确认 */
    int SUCCESS = 0;
    /** 失败 */
    int FAILURE = 1;
    /** 消息有误 */
    int MESSAGE_ERROR = 2;
    /** 不支持 */
    int NOT_SUPPORT = 3;
    /** 报警处理确认 */
    int ALARM_ACK = 4;

    // ===== 终端注册应答结果（T8100） =====
    /** 注册成功 */
    int REGISTER_SUCCESS = 0;
    /** 车辆已被注册 */
    int VEHICLE_ALREADY_REGISTERED = 1;
    /** 数据库中无该车辆 */
    int VEHICLE_NOT_FOUND = 2;
    /** 终端已被注册 */
    int TERMINAL_ALREADY_REGISTERED = 3;
    /** 数据库中无该终端 */
    int TERMINAL_NOT_FOUND = 4;
}
