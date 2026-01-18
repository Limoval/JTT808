package com.lk.jtt808.device.handler.inbound;


import com.lk.jtt808.protocol.entity.T0002;
import com.lk.jtt808.protocol.entity.T8001;
import com.lk.jtt808.device.session.Session;
import com.lk.jtt808.device.session.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 终端心跳处理器
 * 处理终端发送的心跳消息并返回平台通用应答
 */
@Service
@Slf4j
public class HeartbeatHandler extends AbstractInboundHandler<T0002> {

    private static final String HEARTBEAT_KEY_PREFIX = "device:heartbeat:";

    public HeartbeatHandler(RedisTemplate<String, Object> redisTemplate, SessionManager sessionManager) {
        super(redisTemplate, sessionManager);
    }

    @Override
    public Integer handle(T0002 message, Session session) {
        String clientId = message.getClientId();
        log.debug("心跳: clientId={}", clientId);

        // 更新设备心跳时间到Redis
        updateHeartbeatTime(clientId);

        // 发送平台通用应答
        reply(message, session, T8001.Success);

        return 0;
    }

    /**
     * 更新设备心跳时间
     */
    private void updateHeartbeatTime(String clientId) {
        try {
            String key = HEARTBEAT_KEY_PREFIX + clientId;
            redisTemplate.opsForValue().set(key, System.currentTimeMillis(), Duration.ofMinutes(5));
        } catch (Exception e) {
            log.error("更新心跳时间失败: clientId={}", clientId, e);
        }
    }

    @Override
    public Class<T0002> getHandleType() {
        return T0002.class;
    }
}
