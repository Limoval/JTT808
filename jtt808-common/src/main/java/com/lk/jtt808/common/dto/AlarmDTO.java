package com.lk.jtt808.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警信息DTO
 */
@Data
public class AlarmDTO {

    /**
     * 告警ID
     */
    private Long id;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 告警类型
     */
    private Integer alarmType;

    /**
     * 告警类型名称
     */
    private String alarmTypeName;

    /**
     * 告警级别 1:紧急 2:重要 3:一般
     */
    private Integer alarmLevel;

    /**
     * 告警内容
     */
    private String alarmContent;

    /**
     * 告警位置纬度
     */
    private Double latitude;

    /**
     * 告警位置经度
     */
    private Double longitude;

    /**
     * 告警时间
     */
    private LocalDateTime alarmTime;

    /**
     * 是否已处理
     */
    private Boolean handled;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

    /**
     * 处理人
     */
    private String handleUser;

    /**
     * 处理备注
     */
    private String handleRemark;
}
