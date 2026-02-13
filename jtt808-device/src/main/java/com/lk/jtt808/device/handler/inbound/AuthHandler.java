package com.lk.jtt808.device.handler.inbound;


import com.lk.jtt808.device.session.SessionManager;
import com.lk.jtt808.device.transport.TransportSession;
import com.lk.jtt808.protocol.entity.T0102;
import com.lk.jtt808.protocol.entity.T8001;
import com.lk.jtt808.protocol.entity.enums.SessionKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@Slf4j
public class AuthHandler extends AbstractInboundHandler<T0102> {

    public AuthHandler(SessionManager sessionManager) {
        super(sessionManager);
    }

    @Override
    public Integer handle(T0102 msg, TransportSession session) {
        log.info("收到终端鉴权: {}", msg);
        String clientId = msg.getClientId();

        if (!session.isRegistered()) {
            log.warn("设备未注册就发送鉴权: clientId={}", clientId);
            session.close();
            return 1;
        }

        try {
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
