package com.lk.jtt808.device.service;

import com.lk.jtt808.common.entity.LocationRecord;
import com.lk.jtt808.device.repository.LocationRepository;
import com.lk.jtt808.protocol.entity.T0200;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationPersistenceService {

    private final LocationRepository locationRepository;

    @Async("taskExecutor")
    public void saveLocation(T0200 message) {
        try {
            String clientId = message.getClientId();

            // 保存最新位置到 Redis 缓存
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("latitude", message.getLatitude());
            locationData.put("longitude", message.getLongitude());
            locationData.put("speed", message.getSpeed());
            locationData.put("direction", message.getDirection());
            locationData.put("altitude", message.getAltitude());
            locationData.put("timestamp", System.currentTimeMillis());
            locationRepository.saveLocationToCache(clientId, locationData);

            // 解析终端上报时间（BCD格式 yyMMddHHmmss），失败则使用服务器时间
            LocalDateTime locationTime = parseTerminalTime(message.getTime());

            // 持久化到 MySQL
            LocationRecord record = new LocationRecord();
            record.setDeviceId(clientId);
            record.setLatitude(message.getLatitude() != null
                    ? BigDecimal.valueOf(message.getLatitude()) : null);
            record.setLongitude(message.getLongitude() != null
                    ? BigDecimal.valueOf(message.getLongitude()) : null);
            record.setAltitude(message.getAltitude());
            record.setSpeed(message.getSpeed());
            record.setDirection(message.getDirection());
            record.setAlarmFlag(message.getAlarmFlag());
            record.setStatusFlag(message.getStatusFlag());
            record.setLocationTime(locationTime);
            record.setCreateTime(LocalDateTime.now());
            locationRepository.saveLocation(record);

            log.debug("位置数据已保存: clientId={}", clientId);
        } catch (Exception e) {
            log.error("保存位置数据失败: clientId={}", message.getClientId(), e);
        }
    }

    private LocalDateTime parseTerminalTime(String timeStr) {
        if (timeStr == null || timeStr.length() != 12) {
            return LocalDateTime.now();
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMddHHmmss");
            return LocalDateTime.parse(timeStr, formatter);
        } catch (Exception e) {
            log.warn("解析终端时间失败: {}, 使用服务器时间", timeStr);
            return LocalDateTime.now();
        }
    }
}
