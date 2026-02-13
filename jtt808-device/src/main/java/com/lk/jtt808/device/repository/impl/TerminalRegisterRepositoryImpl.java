package com.lk.jtt808.device.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lk.jtt808.common.entity.TerminalRegister;
import com.lk.jtt808.device.mapper.TerminalRegisterMapper;
import com.lk.jtt808.device.repository.TerminalRegisterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class TerminalRegisterRepositoryImpl implements TerminalRegisterRepository {

    private final TerminalRegisterMapper terminalRegisterMapper;

    public TerminalRegisterRepositoryImpl(TerminalRegisterMapper terminalRegisterMapper) {
        this.terminalRegisterMapper = terminalRegisterMapper;
    }

    @Override
    public void save(TerminalRegister register) {
        try {
            terminalRegisterMapper.insert(register);
        } catch (Exception e) {
            log.error("保存终端注册信息失败: deviceId={}", register.getDeviceId(), e);
        }
    }

    @Override
    public TerminalRegister findByDeviceId(String deviceId) {
        return terminalRegisterMapper.selectOne(
                new LambdaQueryWrapper<TerminalRegister>()
                        .eq(TerminalRegister::getDeviceId, deviceId));
    }

    @Override
    public boolean existsByDeviceId(String deviceId) {
        return terminalRegisterMapper.selectCount(
                new LambdaQueryWrapper<TerminalRegister>()
                        .eq(TerminalRegister::getDeviceId, deviceId)) > 0;
    }
}
