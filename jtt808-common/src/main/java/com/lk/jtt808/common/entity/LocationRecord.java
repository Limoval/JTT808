package com.lk.jtt808.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 位置记录表
 */
@Data
@TableName("location_record")
public class LocationRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("device_id")
    private String deviceId;          // 终端手机号

    @TableField("latitude")
    private BigDecimal latitude;      // 纬度

    @TableField("longitude")
    private BigDecimal longitude;     // 经度

    @TableField("altitude")
    private Integer altitude;         // 海拔高度(米)

    @TableField("speed")
    private Integer speed;            // 速度(1/10km/h)

    @TableField("direction")
    private Integer direction;        // 方向(0-359)

    @TableField("location_time")
    private LocalDateTime locationTime; // 位置时间

    @TableField("alarm_flag")
    private Long alarmFlag;          // 报警标志

    @TableField("status_flag")
    private Long statusFlag;         // 状态标志

    @TableField("altitude_flag")
    private Integer altitudeFlag;     // 高度定位是否有效

    @TableField("latitude_flag")
    private Integer latitudeFlag;     // 纬度定位是否有效

    @TableField("longitude_flag")
    private Integer longitudeFlag;    // 经度定位是否有效

    @TableField("speed_flag")
    private Integer speedFlag;        // 速度定位是否有效

    @TableField("direction_flag")
    private Integer directionFlag;    // 方向定位是否有效

    @TableField("create_time")
    private LocalDateTime createTime; // 创建时间

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTimeFill; // MyBatis Plus 自动填充
}