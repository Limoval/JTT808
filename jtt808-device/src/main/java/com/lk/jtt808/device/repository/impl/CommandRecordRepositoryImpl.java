package com.lk.jtt808.device.repository.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lk.jtt808.common.entity.CommandRecord;
import com.lk.jtt808.device.mapper.CommandRecordMapper;
import com.lk.jtt808.device.repository.CommandRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

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
        }
    }

    @Override
    public void updateResult(Long commandId, Integer result) {
        try {
            commandRecordMapper.update(null, new LambdaUpdateWrapper<CommandRecord>()
                    .eq(CommandRecord::getId, commandId)
                    .set(CommandRecord::getResult, result)
                    .set(CommandRecord::getResponseTime, LocalDateTime.now()));
        } catch (Exception e) {
            log.error("更新命令结果失败: commandId={}", commandId, e);
        }
    }
}
