package com.lk.jtt808.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备表
 */
@Data
@TableName("device")
public class Device {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("device_id")
    private String deviceId;          // 终端手机号

    @TableField("device_name")
    private String deviceName;        // 设备名称

    @TableField("phone_number")
    private String phoneNumber;       // 手机号码

    @TableField("auth_code")
    private String authCode;          // 鉴权码

    @TableField("device_type")
    private String deviceType;        // 设备类型

    @TableField("manufacturer")
    private String manufacturer;      // 制造商ID

    @TableField("model")
    private String model;            // 设备型号

    @TableField("protocol_version")
    private String protocolVersion;   // 协议版本号

    @TableField("status")
    private Integer status;           // 0:离线 1:在线

    @TableField("last_heartbeat")
    private LocalDateTime lastHeartbeat; // 最后心跳时间

    @TableField("last_location_time")
    private LocalDateTime lastLocationTime; // 最后定位时间

    @TableField("create_time")
    private LocalDateTime createTime; // 创建时间

    @TableField("update_time")
    private LocalDateTime updateTime; // 更新时间

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTimeFill; // MyBatis Plus 自动填充

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTimeFill; // MyBatis Plus 自动填充
}