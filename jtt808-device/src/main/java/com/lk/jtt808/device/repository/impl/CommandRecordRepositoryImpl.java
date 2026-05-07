package com.lk.jtt808.device.repository.impl;

import com.lk.jtt808.common.entity.CommandRecord;
import com.lk.jtt808.device.mapper.CommandRecordMapper;
import com.lk.jtt808.device.repository.CommandRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class CommandRecordRepositoryImpl implements CommandRecordRepository {

    private final CommandRecordMapper commandRecordMapper;

    public CommandRecordRepositoryImpl(CommandRecordMapper commandRecordMapper) {
        this.commandRecordMapper = commandRecordMapper;
    }

    @Override
    public void saveCommand(CommandRecord record) {
        try {
            commandRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("保存命令记录失败: deviceId={}", record.getDeviceId(), e);
            throw e;
        }
    }

    @Override
    public void updateCommand(CommandRecord record) {
        try {
            commandRecordMapper.updateById(record);
        } catch (Exception e) {
            log.error("更新命令记录失败: commandId={}", record.getId(), e);
            throw e;
        }
    }

    @Override
    public CommandRecord findById(Long commandId) {
        return commandRecordMapper.selectById(commandId);
    }
}
