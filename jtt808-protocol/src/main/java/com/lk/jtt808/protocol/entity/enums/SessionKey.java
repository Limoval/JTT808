package com.lk.jtt808.protocol.entity.enums;

/**
 * 会话属性键枚举
 * 配合SessionManager的EnumMap优化
 */
public enum SessionKey {
    /** 设备信息 */
    DEVICE,
    /** 鉴权码 */
    AUTH_CODE,
    /** 最后位置信息 */
    LAST_LOCATION,
    /** 设备状态 */
    DEVICE_STATUS,
    /** 协议版本 */
    PROTOCOL_VERSION,
    /** 最后心跳时间 */
    LAST_HEARTBEAT_TIME,
    /** 是否已通过鉴权 */
    AUTHENTICATED
}
