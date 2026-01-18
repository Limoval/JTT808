package com.lk.jtt808.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lk.jtt808.admin.mapper.AlarmRecordMapper;
import com.lk.jtt808.admin.service.AlarmService;
import com.lk.jtt808.common.dto.AlarmDTO;
import com.lk.jtt808.common.entity.AlarmRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 告警服务实现
 */
@Service
@RequiredArgsConstructor
public class AlarmServiceImpl implements AlarmService {

    private final AlarmRecordMapper alarmRecordMapper;

    @Override
    public Page<AlarmRecord> listAlarms(Integer page, Integer size, String deviceId, Integer alarmType, Boolean handled) {
        Page<AlarmRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(deviceId)) {
            wrapper.eq(AlarmRecord::getDeviceId, deviceId);
        }
        if (alarmType != null) {
            wrapper.eq(AlarmRecord::getAlarmType, alarmType);
        }
        if (handled != null) {
            wrapper.eq(AlarmRecord::getHandled, handled);
        }
        wrapper.orderByDesc(AlarmRecord::getAlarmTime);

        return alarmRecordMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public AlarmRecord getAlarmById(Long id) {
        return alarmRecordMapper.selectById(id);
    }

    @Override
    public boolean handleAlarm(Long id, String handleUser, String handleRemark) {
        AlarmRecord record = alarmRecordMapper.selectById(id);
        if (record == null) {
            return false;
        }
        record.setHandled(true);
        record.setHandleTime(LocalDateTime.now());
        record.setHandleUser(handleUser);
        record.setHandleRemark(handleRemark);
        return alarmRecordMapper.updateById(record) > 0;
    }

    @Override
    public AlarmDTO toDTO(AlarmRecord record) {
        if (record == null) {
            return null;
        }
        AlarmDTO dto = new AlarmDTO();
        dto.setId(record.getId());
        dto.setDeviceId(record.getDeviceId());
        dto.setAlarmType(record.getAlarmType());
        dto.setAlarmLevel(record.getAlarmLevel());
        dto.setAlarmContent(record.getAlarmContent());
        dto.setLatitude(record.getLatitude() != null ? record.getLatitude().doubleValue() : null);
        dto.setLongitude(record.getLongitude() != null ? record.getLongitude().doubleValue() : null);
        dto.setAlarmTime(record.getAlarmTime());
        dto.setHandled(record.getHandled());
        dto.setHandleTime(record.getHandleTime());
        dto.setHandleUser(record.getHandleUser());
        dto.setHandleRemark(record.getHandleRemark());
        return dto;
    }
}
