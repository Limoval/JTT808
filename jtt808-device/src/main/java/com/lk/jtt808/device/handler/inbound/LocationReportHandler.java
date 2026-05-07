package com.lk.jtt808.device.handler.inbound;


import com.lk.jtt808.common.entity.AlarmRecord;
import com.lk.jtt808.device.repository.AlarmRepository;
import com.lk.jtt808.device.repository.LocationRepository;
import com.lk.jtt808.device.service.LocationPersistenceService;
import com.lk.jtt808.device.session.SessionManager;
import com.lk.jtt808.device.transport.TransportSession;
import com.lk.jtt808.protocol.entity.T0200;
import com.lk.jtt808.protocol.entity.T8001;
import com.lk.jtt808.protocol.entity.enums.SessionKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Service
@Slf4j
public class LocationReportHandler extends AbstractInboundHandler<T0200> {

    private final LocationRepository locationRepository;
    private final AlarmRepository alarmRepository;
    private final LocationPersistenceService locationPersistenceService;

    public LocationReportHandler(SessionManager sessionManager,
                                 LocationRepository locationRepository,
                                 AlarmRepository alarmRepository,
                                 LocationPersistenceService locationPersistenceService) {
        super(sessionManager);
        this.locationRepository = locationRepository;
        this.alarmRepository = alarmRepository;
        this.locationPersistenceService = locationPersistenceService;
    }

    @Override
    public Integer handle(T0200 message, TransportSession session) {
        String clientId = message.getClientId();

        // 未鉴权设备不处理位置上报
        if (!Boolean.TRUE.equals(session.getAttribute(SessionKey.AUTHENTICATED))) {
            log.warn("未鉴权设备发送位置上报，忽略: clientId={}", clientId);
            reply(message, session, T8001.Failure);
            return 1;
        }

        log.info("位置上报: clientId={}, lat={}, lon={}, speed={}, direction={}",
                clientId,
                message.getLatitude(),
                message.getLongitude(),
                message.getSpeed(),
                message.getDirection());

        // 1. 异步保存位置数据
        locationPersistenceService.saveLocation(message);

        // 2. 检查报警标志
        if (message.getAlarmFlag() != null && message.getAlarmFlag() != 0) {
            processAlarms(message);
        }

        // 3. 发送平台通用应答
        reply(message, session, T8001.Success);

        return 0;
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
