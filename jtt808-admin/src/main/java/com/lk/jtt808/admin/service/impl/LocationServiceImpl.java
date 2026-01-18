package com.lk.jtt808.admin.service.impl;

import com.lk.jtt808.admin.mapper.LocationRecordMapper;
import com.lk.jtt808.admin.service.LocationService;
import com.lk.jtt808.common.dto.LocationDTO;
import com.lk.jtt808.common.entity.LocationRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 位置服务实现
 */
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRecordMapper locationRecordMapper;

    @Override
    public LocationRecord getLatestLocation(String deviceId) {
        return locationRecordMapper.selectLatestByDeviceId(deviceId);
    }

    @Override
    public List<LocationRecord> getLocationHistory(String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        return locationRecordMapper.selectHistory(deviceId, startTime, endTime);
    }

    @Override
    public LocationDTO toDTO(LocationRecord record) {
        if (record == null) {
            return null;
        }
        LocationDTO dto = new LocationDTO();
        dto.setDeviceId(record.getDeviceId());
        dto.setLatitude(record.getLatitude());
        dto.setLongitude(record.getLongitude());
        dto.setAltitude(record.getAltitude());
        dto.setSpeed(record.getSpeed());
        dto.setDirection(record.getDirection());
        dto.setLocationTime(record.getLocationTime());
        dto.setAlarmFlag(record.getAlarmFlag());
        dto.setStatusFlag(record.getStatusFlag());
        return dto;
    }
}
