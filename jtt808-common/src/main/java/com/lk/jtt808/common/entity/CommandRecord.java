package com.lk.jtt808.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 命令下发记录表
 */
@Data
@TableName("command_record")
public class CommandRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("device_id")
    private String deviceId;          // 终端手机号

    @TableField("command_type")
    private Integer commandType;      // 命令类型

    @TableField("command_params")
    private String commandParams;     // 命令参数

    @TableField("command_serial_no")
    private Integer commandSerialNo;  // 命令流水号

    @TableField("send_time")
    private LocalDateTime sendTime;   // 发送时间

    @TableField("response_time")
    private LocalDateTime responseTime; // 响应时间

    @TableField("response_code")
    private Integer responseCode;     // 应答流水号

    @TableField("status")
    private Integer status;

    @TableField("result")
    private Integer result;           // 执行结果 0:成功 1:失败 2:消息有误 3:不支持 4:报警处理确认
}
