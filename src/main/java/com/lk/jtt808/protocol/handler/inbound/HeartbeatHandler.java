package com.lk.jtt808.protocol.handler.inbound;


import com.lk.jtt808.protocol.entity.T0002;
import com.lk.jtt808.protocol.session.Session;
import com.lk.jtt808.protocol.session.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author: Limoval
 * time: 2025/6/11 14:49 周三
 * description:
 */
@Service
@Slf4j
public class HeartbeatHandler extends AbstractInboundHandler<T0002> {

    public HeartbeatHandler(RedisTemplate<String, Object> redisTemplate, SessionManager sessionManager) {
        super(redisTemplate, sessionManager);
    }

    @Override
    public Integer handle(T0002 message, Session session) {
        log.info("收到终端心跳: {}", message);
        String clientId = message.getClientId();
        int messageId = message.getMessageId();

        return 0;
    }

    @Override
    public Class<T0002> getHandleType() {
        return T0002.class;
    }
}
