package com.lk.jtt808.device.handler.inbound;


import com.lk.jtt808.common.enums.DeviceStatusEnum;
import com.lk.jtt808.device.repository.DeviceRepository;
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

    private final DeviceRepository deviceRepository;

    public AuthHandler(SessionManager sessionManager,
                       DeviceRepository deviceRepository) {
        super(sessionManager);
        this.deviceRepository = deviceRepository;
    }

    @Override
    public Integer handle(T0102 msg, TransportSession session) {
        log.info("收到终端鉴权: {}", msg);
        String clientId = msg.getClientId();

        if (!session.isRegistered()) {
            log.warn("设备未注册就发送鉴权: clientId={}", clientId);
            sendFailureAndClose(msg, session);
            return 1;
        }

        try {
            String expectedAuthCode = (String) session.getAttribute(SessionKey.AUTH_CODE);
            log.info("鉴权码:{},终端上报鉴权码:{}", expectedAuthCode, msg.getAuthCode());
            if (ObjectUtils.nullSafeEquals(msg.getAuthCode(), expectedAuthCode)) {
                session.setAttribute(SessionKey.AUTHENTICATED, true);
                deviceRepository.updateStatus(clientId, DeviceStatusEnum.ONLINE.getCode());
                reply(msg, session, T8001.Success);
                log.info("设备鉴权成功: clientId={}", clientId);
            } else {
                log.warn("鉴权失败: clientId={}, expected={}, actual={}", clientId, expectedAuthCode, msg.getAuthCode());
                sendFailureAndClose(msg, session);
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

    private void sendFailureAndClose(T0102 msg, TransportSession session) {
        sendReply(msg, session, T8001.Failure)
                .doFinally(signalType -> session.close())
                .subscribe(
                        ignored -> {
                        },
                        error -> log.error("发送鉴权失败应答失败: clientId={}", msg.getClientId(), error));
    }
}
