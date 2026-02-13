package com.lk.jtt808.device.repository;

import com.lk.jtt808.common.entity.TerminalRegister;

/**
 * 终端注册仓储接口
 */
public interface TerminalRegisterRepository {

    /**
     * 保存注册信息
     */
    void save(TerminalRegister register);

    /**
     * 根据设备ID查询注册信息
     */
    TerminalRegister findByDeviceId(String deviceId);

    /**
     * 检查设备是否已注册
     */
    boolean existsByDeviceId(String deviceId);
}
