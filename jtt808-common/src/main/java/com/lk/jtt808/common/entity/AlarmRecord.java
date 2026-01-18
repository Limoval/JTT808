package com.lk.jtt808.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报警记录表
 */
@Data
@TableName("alarm_record")
public class AlarmRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("device_id")
    private String deviceId;          // 终端手机号

    @TableField("alarm_type")
    private Integer alarmType;        // 报警类型

    @TableField("alarm_level")
    private Integer alarmLevel;       // 报警级别 1:紧急 2:重要 3:一般

    @TableField("alarm_content")
    private String alarmContent;      // 报警内容描述

    @TableField("latitude")
    private java.math.BigDecimal latitude;   // 报警位置纬度

    @TableField("longitude")
    private java.math.BigDecimal longitude;  // 报警位置经度

    @TableField("altitude")
    private Integer altitude;         // 报警位置海拔

    @TableField("alarm_time")
    private LocalDateTime alarmTime;  // 报警时间

    @TableField("handled")
    private Boolean handled;          // 是否已处理

    @TableField("handle_time")
    private LocalDateTime handleTime; // 处理时间

    @TableField("handle_user")
    private String handleUser;        // 处理人

    @TableField("handle_remark")
    private String handleRemark;      // 处理备注

    @TableField("create_time")
    private LocalDateTime createTime; // 创建时间

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTimeFill; // MyBatis Plus 自动填充
}