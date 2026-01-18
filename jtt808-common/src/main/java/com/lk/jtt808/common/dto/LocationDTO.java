package com.lk.jtt808.common.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 位置信息DTO
 */
@Data
public class LocationDTO {
    private String deviceId;          // 终端手机号
    private BigDecimal latitude;      // 纬度
    private BigDecimal longitude;     // 经度
    private Integer altitude;         // 海拔高度(米)
    private Integer speed;            // 速度(1/10km/h)
    private Integer direction;        // 方向(0-359)
    private LocalDateTime locationTime; // 位置时间
    private Long alarmFlag;          // 报警标志
    private Long statusFlag;         // 状态标志
}