package com.lk.jtt808.device.session;

import com.lk.jtt808.common.enums.DeviceStatusEnum;
import com.lk.jtt808.device.repository.DeviceRepository;
import com.lk.jtt808.device.transport.TransportSession;
import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.entity.JT808Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.function.BiConsumer;

@Component
@Slf4j
public class SessionListener {

    private final DeviceRepository deviceRepository;

    public SessionListener(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    /**
     * 出站请求消息及回复统一处理拦截器
     */
    private static final BiConsumer<TransportSession, JT808Message> outBoundMessageProcessor = (session, message) -> {

        // 1. 自动设置设备ID
        if (StringUtils.hasLength(session.getClientId())) {
            message.setClientId(session.getClientId());
        }

        // 2. 自动分配出站消息流水号
        message.setOutboundSerialNo(session.nextSerialNo());

        // 3. 自动设置消息ID（如果未设置）
        if (message.getMessageId() == 0) {
            MessageType annotation = message.getClass().getAnnotation(MessageType.class);
            if (annotation != null) {
                message.setMessageId(annotation.value());
            }
        }

        log.debug("消息预处理完成: clientId={}, messageId={}, outboundSerialNo={}",
            message.getClientId(), message.getMessageId(), message.getOutboundSerialNo());
    };


    /**
     * 设备连接建立时的处理
     */
    public void sessionCreated(Session session) {
        log.info("设备连接建立: {}", session.getRemoteAddressStr());
        session.setOutBoundInterceptor(outBoundMessageProcessor);
    }

    /**
     * 设备注册成功时的处理
     * 注意：注册成功不等于鉴权成功，此时设备状态保持 OFFLINE
     */
    public void sessionRegistered(Session session) {
        String clientId = session.getClientId();
        log.info("设备注册成功: clientId={}, address={}", clientId, session.getRemoteAddressStr());
        // 不在这里更新 ONLINE，鉴权成功后再更新
    }

    /**
     * 设备连接断开时的处理
     */
    public void sessionDestroyed(Session session) {
        String clientId = session.getClientId();
        log.info("设备连接断开: clientId={}, address={}", clientId, session.getRemoteAddressStr());
        handleDeviceOffline(session);
    }

    /**
     * 处理设备上线业务逻辑
     */
    private void handleDeviceOnline(Session session) {
        String clientId = session.getClientId();
        deviceRepository.updateStatus(clientId, DeviceStatusEnum.ONLINE.getCode());
        log.info("设备上线处理已提交: {}", clientId);
    }

    /**
     * 处理设备离线业务逻辑
     */
    private void handleDeviceOffline(Session session) {
        String clientId = session.getClientId();
        deviceRepository.updateStatus(clientId, DeviceStatusEnum.OFFLINE.getCode());
        log.info("设备离线处理已提交: {}", clientId);
    }
}
