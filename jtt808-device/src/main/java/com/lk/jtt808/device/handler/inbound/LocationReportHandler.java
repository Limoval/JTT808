package com.lk.jtt808.device.handler.inbound;


import com.lk.jtt808.protocol.entity.T0200;
import com.lk.jtt808.protocol.entity.T8001;
import com.lk.jtt808.device.session.Session;
import com.lk.jtt808.device.session.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class LocationReportHandler extends AbstractInboundHandler<T0200> {

    public LocationReportHandler(RedisTemplate<String, Object> redisTemplate, SessionManager sessionManager) {
        super(redisTemplate, sessionManager);
    }

    @Override
    public Integer handle(T0200 message, Session session) {
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
            processAlarms(message, session);
        }

        // 3. 发送平台通用应答
        reply(message, session, T8001.Success);

        return 0;
    }

    /**
     * 异步保存位置数据到Redis和数据库
     */
    @Async
    protected void saveLocationAsync(T0200 message) {
        try {
            String clientId = message.getClientId();

            // 保存最新位置到Redis
            String locationKey = "device:location:" + clientId;
            redisTemplate.opsForHash().put(locationKey, "latitude", message.getLatitude());
            redisTemplate.opsForHash().put(locationKey, "longitude", message.getLongitude());
            redisTemplate.opsForHash().put(locationKey, "speed", message.getSpeed());
            redisTemplate.opsForHash().put(locationKey, "direction", message.getDirection());
            redisTemplate.opsForHash().put(locationKey, "altitude", message.getAltitude());
            redisTemplate.opsForHash().put(locationKey, "timestamp", System.currentTimeMillis());

            log.debug("位置数据已保存到Redis: clientId={}", clientId);
        } catch (Exception e) {
            log.error("保存位置数据失败: clientId={}", message.getClientId(), e);
        }
    }

    /**
     * 处理报警信息
     */
    private void processAlarms(T0200 message, Session session) {
        long alarmFlag = message.getAlarmFlag();
        String clientId = message.getClientId();

        log.warn("检测到报警: clientId={}, alarmFlag=0x{}", clientId, Long.toHexString(alarmFlag));

        // 将报警信息保存到Redis（可以后续由告警服务处理）
        String alarmKey = "device:alarm:" + clientId + ":" + System.currentTimeMillis();
        redisTemplate.opsForHash().put(alarmKey, "alarmFlag", alarmFlag);
        redisTemplate.opsForHash().put(alarmKey, "latitude", message.getLatitude());
        redisTemplate.opsForHash().put(alarmKey, "longitude", message.getLongitude());
        redisTemplate.opsForHash().put(alarmKey, "timestamp", System.currentTimeMillis());
    }

    @Override
    public Class<T0200> getHandleType() {
        return T0200.class;
    }
}
