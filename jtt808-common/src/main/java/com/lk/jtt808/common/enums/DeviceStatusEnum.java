package com.lk.jtt808.common.enums;

/**
 * 设备状态枚举
 */
public enum DeviceStatusEnum {
    OFFLINE(0, "离线"),
    ONLINE(1, "在线"),
    SLEEP(2, "休眠"),
    FAULT(3, "故障");

    private final Integer code;
    private final String desc;

    DeviceStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static DeviceStatusEnum getByCode(Integer code) {
        for (DeviceStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return OFFLINE;
    }
}