package com.lk.jtt808.common.dto;

import lombok.Data;

/**
 * 命令下发DTO
 */
@Data
public class CommandDTO {

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 命令类型
     */
    private Integer commandType;

    /**
     * 命令参数（JSON格式）
     */
    private String commandParams;

    /**
     * 超时时间（秒）
     */
    private Integer timeout;
}
