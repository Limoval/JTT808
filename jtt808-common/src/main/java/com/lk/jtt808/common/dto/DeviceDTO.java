package com.lk.jtt808.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备信息DTO
 */
@Data
public class DeviceDTO {
    private String deviceId;          // 终端手机号
    private String deviceName;        // 设备名称
    private String authCode;          // 鉴权码
    private Integer status;           // 0:离线 1:在线
    private LocalDateTime lastHeartbeat; // 最后心跳时间
    private LocalDateTime createdTime; // 创建时间
    private String protocolVersion;   // 协议版本
    private String manufacturer;      // 厂商ID
    private String model;            // 设备型号
}