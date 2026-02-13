package com.lk.jtt808.device.handler.inbound;


import com.lk.jtt808.common.entity.AlarmRecord;
import com.lk.jtt808.common.entity.LocationRecord;
import com.lk.jtt808.device.repository.AlarmRepository;
import com.lk.jtt808.device.repository.LocationRepository;
import com.lk.jtt808.device.session.SessionManager;
import com.lk.jtt808.device.transport.TransportSession;
import com.lk.jtt808.protocol.entity.T0200;
import com.lk.jtt808.protocol.entity.T8001;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Service
@Slf4j
public class LocationReportHandler extends AbstractInboundHandler<T0200> {

    private final LocationRepository locationRepository;
    private final AlarmRepository alarmRepository;

    public LocationReportHandler(SessionManager sessionManager,
                                 LocationRepository locationRepository,
                                 AlarmRepository alarmRepository) {
        super(sessionManager);
        this.locationRepository = locationRepository;
        this.alarmRepository = alarmRepository;
    }

    @Override
    public Integer handle(T0200 message, TransportSession session) {
        log.info("位置上报: clientId={}, lat={}, lon={}, speed={}, direction={}",
                message.getClientId(),
                message.getLatitude(),
                message.getLongitude(),
                message.getSpeed(),
                message.getDirection());

        // 1. 异步保存位置数据
        saveLocationAsync(message);

        // 2. 检查报警标志
        if (message.getAlarmFlag() != null && message.getAlarmFlag() != 0) {
            processAlarms(message);
        }

        // 3. 发送平台通用应答
        reply(message, session, T8001.Success);

        return 0;
    }

    /**
     * 异步保存位置数据到 Redis 缓存和 MySQL 持久化
     */
    @Async
    protected void saveLocationAsync(T0200 message) {
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
            record.setLocationTime(LocalDateTime.now());
            record.setCreateTime(LocalDateTime.now());
            locationRepository.saveLocation(record);

            log.debug("位置数据已保存: clientId={}", clientId);
        } catch (Exception e) {
            log.error("保存位置数据失败: clientId={}", message.getClientId(), e);
        }
    }

    /**
     * 处理报警信息，直接写入 MySQL（不能丢失）
     */
    private void processAlarms(T0200 message) {
        long alarmFlag = message.getAlarmFlag();
        String clientId = message.getClientId();

        log.warn("检测到报警: clientId={}, alarmFlag=0x{}", clientId, Long.toHexString(alarmFlag));

        AlarmRecord record = new AlarmRecord();
        record.setDeviceId(clientId);
        record.setAlarmType((int) alarmFlag);
        record.setAlarmLevel(2); // 默认重要级别
        record.setAlarmContent("报警标志: 0x" + Long.toHexString(alarmFlag));
        record.setLatitude(message.getLatitude() != null
                ? BigDecimal.valueOf(message.getLatitude()) : null);
        record.setLongitude(message.getLongitude() != null
                ? BigDecimal.valueOf(message.getLongitude()) : null);
        record.setAltitude(message.getAltitude());
        record.setAlarmTime(LocalDateTime.now());
        record.setHandled(false);
        record.setCreateTime(LocalDateTime.now());

        alarmRepository.saveAlarm(record);
    }

    @Override
    public Class<T0200> getHandleType() {
        return T0200.class;
    }
}
