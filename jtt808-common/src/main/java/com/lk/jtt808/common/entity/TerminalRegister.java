package com.lk.jtt808.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 终端注册表
 */
@Data
@TableName("terminal_register")
public class TerminalRegister {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("device_id")
    private String deviceId;              // 终端手机号

    @TableField("province_id")
    private String provinceId;            // 省域ID

    @TableField("city_id")
    private String cityId;                // 市县域ID

    @TableField("manufacturer_id")
    private String manufacturerId;        // 制造商ID

    @TableField("terminal_model")
    private String terminalModel;         // 终端型号

    @TableField("terminal_id")
    private String terminalId;            // 终端ID

    @TableField("license_plate_color")
    private Integer licensePlateColor;    // 车牌颜色

    @TableField("license_plate_number")
    private String licensePlateNumber;    // 车牌号码

    @TableField("response_code")
    private Integer responseCode;         // 应答流水号

    @TableField("result")
    private Integer result;               // 注册结果 0:成功 1:失败

    @TableField("verify_code")
    private String verifyCode;            // 鉴权码

    @TableField("register_time")
    private LocalDateTime registerTime;   // 注册时间
}
