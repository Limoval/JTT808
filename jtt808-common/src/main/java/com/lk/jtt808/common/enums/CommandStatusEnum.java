package com.lk.jtt808.common.enums;

/**
 * 命令状态枚举
 */
public enum CommandStatusEnum {
    PENDING(0, "待发送"),
    SENT(1, "已发送"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败"),
    TIMEOUT(4, "超时");

    private final Integer code;
    private final String desc;

    CommandStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static CommandStatusEnum getByCode(Integer code) {
        for (CommandStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return PENDING;
    }
}
