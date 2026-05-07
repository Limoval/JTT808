package com.lk.jtt808.device.service;


import com.lk.jtt808.common.entity.CommandRecord;
import com.lk.jtt808.protocol.entity.JT808Message;
import com.lk.jtt808.protocol.entity.JT808Response;

import java.time.Duration;

public interface CommandService {

    /**
     * 发送命令到设备并跟踪状态
     *
     * @param deviceId       设备ID
     * @param message        JT808消息
     * @param responseClass  期望的响应类型
     * @param timeout        超时时间
     * @return 命令记录ID
     */
    Long sendCommand(String deviceId, JT808Message message, Class<? extends JT808Response> responseClass, Duration timeout);

    /**
     * 获取命令状态
     *
     * @param commandId 命令ID
     * @return 命令记录
     */
    CommandRecord getCommandStatus(Long commandId);

}
