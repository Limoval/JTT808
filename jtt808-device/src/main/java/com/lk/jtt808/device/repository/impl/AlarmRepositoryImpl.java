package com.lk.jtt808.device.repository.impl;

import com.lk.jtt808.common.entity.AlarmRecord;
import com.lk.jtt808.device.mapper.AlarmRecordMapper;
import com.lk.jtt808.device.repository.AlarmRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class AlarmRepositoryImpl implements AlarmRepository {

    private final AlarmRecordMapper alarmRecordMapper;

    public AlarmRepositoryImpl(AlarmRecordMapper alarmRecordMapper) {
        this.alarmRecordMapper = alarmRecordMapper;
    }

    @Override
    public void saveAlarm(AlarmRecord record) {
        try {
            alarmRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("保存报警记录到MySQL失败: deviceId={}", record.getDeviceId(), e);
        }
    }
}
