package com.lk.jtt808.device.repository;

import com.lk.jtt808.common.entity.AlarmRecord;

/**
 * 报警仓储接口
 * 报警数据直接写入 MySQL（不能丢失）
 */
public interface AlarmRepository {

    /**
     * 保存报警记录
     */
    void saveAlarm(AlarmRecord record);
}
