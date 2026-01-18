package com.lk.jtt808.device.handler.inbound;


import com.lk.jtt808.protocol.entity.T0200;
import com.lk.jtt808.device.session.Session;
import com.lk.jtt808.device.session.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class LocationReportHandler extends AbstractInboundHandler<T0200> {
    public LocationReportHandler(RedisTemplate<String, Object> redisTemplate, SessionManager sessionManager) {
        super(redisTemplate, sessionManager);
    }

    @Override
    public Integer handle(T0200 message, Session session) {
        log.info("收到0x0200位置信息: {}", message);
        String clientId = message.getClientId();
        Double longitude = message.getLongitude();
        Double latitude = message.getLatitude();
        //查询redis里是否已经存在该clientId,如果存在则证明已经进行过clientId新增操作
        Boolean b = redisTemplate
                .opsForValue()
                .setIfAbsent(MQTT_MESSAGE_KEY + clientId, clientId);

        return 0;
    }

    @Override
    public Class<T0200> getHandleType() {
        return T0200.class;
    }
}
