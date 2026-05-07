package com.lk.jtt808.device.handler.inbound;


import com.lk.jtt808.device.repository.DeviceRepository;
import com.lk.jtt808.device.session.SessionManager;
import com.lk.jtt808.device.transport.TransportSession;
import com.lk.jtt808.protocol.entity.T0002;
import com.lk.jtt808.protocol.entity.T8001;
import com.lk.jtt808.protocol.entity.enums.SessionKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 终端心跳处理器
 * 通过 DeviceRepository 更新心跳时间
 */
@Service
@Slf4j
public class HeartbeatHandler extends AbstractInboundHandler<T0002> {

    private final DeviceRepository deviceRepository;

    public HeartbeatHandler(SessionManager sessionManager,
                            DeviceRepository deviceRepository) {
        super(sessionManager);
        this.deviceRepository = deviceRepository;
    }

    @Override
    public Integer handle(T0002 message, TransportSession session) {
        String clientId = message.getClientId();

        // 未鉴权设备不处理心跳
        if (!Boolean.TRUE.equals(session.getAttribute(SessionKey.AUTHENTICATED))) {
            log.warn("未鉴权设备发送心跳，忽略: clientId={}", clientId);
            reply(message, session, T8001.Failure);
            return 1;
        }

        log.debug("心跳: clientId={}", clientId);

        // 通过 Repository 更新心跳时间（Redis 缓存 + 异步 MySQL）
        deviceRepository.updateHeartbeat(clientId);

        // 发送平台通用应答
        reply(message, session, T8001.Success);

        return 0;
    }

    @Override
    public Class<T0002> getHandleType() {
        return T0002.class;
    }
}
