package com.lk.jtt808.device.repository;

import com.lk.jtt808.common.entity.CommandRecord;

/**
 * 命令记录仓储接口
 */
public interface CommandRecordRepository {

    /**
     * 保存命令记录
     */
    void saveCommand(CommandRecord record);

    /**
     * 更新命令执行结果
     */
    void updateResult(Long commandId, Integer result);
}
