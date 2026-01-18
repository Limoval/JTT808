package com.lk.jtt808.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lk.jtt808.common.dto.AlarmDTO;
import com.lk.jtt808.common.entity.AlarmRecord;

/**
 * 告警服务接口
 */
public interface AlarmService {

    /**
     * 分页查询告警列表
     */
    Page<AlarmRecord> listAlarms(Integer page, Integer size, String deviceId, Integer alarmType, Boolean handled);

    /**
     * 获取告警详情
     */
    AlarmRecord getAlarmById(Long id);

    /**
     * 处理告警
     */
    boolean handleAlarm(Long id, String handleUser, String handleRemark);

    /**
     * 转换为DTO
     */
    AlarmDTO toDTO(AlarmRecord record);
}
