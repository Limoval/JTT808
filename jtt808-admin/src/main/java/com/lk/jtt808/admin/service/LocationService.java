package com.lk.jtt808.admin.service;

import com.lk.jtt808.common.dto.LocationDTO;
import com.lk.jtt808.common.entity.LocationRecord;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 位置服务接口
 */
public interface LocationService {

    /**
     * 获取设备最新位置
     */
    LocationRecord getLatestLocation(String deviceId);

    /**
     * 查询历史轨迹
     */
    List<LocationRecord> getLocationHistory(String deviceId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 转换为DTO
     */
    LocationDTO toDTO(LocationRecord record);
}
