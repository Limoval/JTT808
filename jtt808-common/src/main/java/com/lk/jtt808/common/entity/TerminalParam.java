package com.lk.jtt808.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 终端参数表
 */
@Data
@TableName("terminal_param")
public class TerminalParam {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("device_id")
    private String deviceId;          // 终端手机号

    @TableField("param_id")
    private Integer paramId;          // 参数ID

    @TableField("param_length")
    private Integer paramLength;      // 参数长度

    @TableField("param_value")
    private String paramValue;        // 参数值

    @TableField("update_time")
    private LocalDateTime updateTime; // 更新时间
}
