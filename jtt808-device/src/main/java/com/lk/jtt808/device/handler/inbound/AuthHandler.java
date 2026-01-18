package com.lk.jtt808.device.handler.inbound;


import com.lk.jtt808.protocol.entity.T0102;
import com.lk.jtt808.protocol.entity.T8001;
import com.lk.jtt808.protocol.entity.enums.SessionKey;
import com.lk.jtt808.device.session.Session;
import com.lk.jtt808.device.session.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

/**
 * @author: Limoval
 * time: 2025/6/11 14:44 周三
 * description:
 */
@Service
@Slf4j
public class AuthHandler extends AbstractInboundHandler<T0102> {

    public AuthHandler(RedisTemplate<String, Object> redisTemplate, SessionManager sessionManager) {
        super(redisTemplate, sessionManager);
    }

    @Override
    public Integer handle(T0102 msg, Session session) {
        log.info("收到终端鉴权: {}", msg);
        String clientId = msg.getClientId();

        if (!session.isRegistered()) {
            log.warn("设备未注册就发送鉴权: clientId={}", clientId);
            session.getChannel().close();
            return 1;
        }

        try {
            //验证鉴权码
            String expectedAuthCode = (String) session.getAttribute(SessionKey.AUTH_CODE);
            log.info("鉴权码:{},终端上报鉴权码:{}", expectedAuthCode, msg.getAuthCode());
            if (ObjectUtils.nullSafeEquals(msg.getAuthCode(), expectedAuthCode)) {
                reply(msg, session, T8001.Success);
            } else {
                reply(msg, session, T8001.Failure);
            }

        } catch (Exception e) {
            log.error("处理终端鉴权异常: clientId={}", clientId, e);
        }
        return 0;
    }

    @Override
    public Class<T0102> getHandleType() {
        return T0102.class;
    }
}
