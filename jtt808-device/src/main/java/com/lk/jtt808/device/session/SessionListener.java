package com.lk.jtt808.device.session;

import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.entity.JT808Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Component
@Slf4j
public class SessionListener {


    /**
     * 出站请求消息及回复统一处理拦截器
     */
    private static final BiConsumer<Session, JT808Message> outBoundMessageProcessor = (session, message) -> {

        // 1. 自动设置设备ID
        if (StringUtils.hasLength(session.getClientId())) {
            message.setClientId(session.getClientId());
        }

        // 2. 自动分配出站消息流水号
        message.setOutboundSerialNo(session.nextSerialNo());

        // 3. 自动设置消息ID（如果未设置）
        if (message.getMessageId() == 0) {
            // 通过反射获取消息类对应的消息ID
            MessageType annotation = message.getClass().getAnnotation(MessageType.class);
            int value = annotation.value();
            message.setMessageId(value);
        }

        log.info("消息预处理完成: clientId={}, messageId={}, outboundSerialNo={}",
            message.getClientId(), message.getMessageId(), message.getOutboundSerialNo());
    };


    /**
     * 设备连接建立时的处理
     * 在TCP连接建立但设备尚未注册时触发
     *
     * @param session 新建立的会话
     */
    public void sessionCreated(Session session) {
        log.info("设备连接建立: {}", session.getRemoteAddressStr());

        // 设置下行消息处理拦截器
        session.setOutBoundInterceptor(outBoundMessageProcessor);

        // 可以在这里添加其他连接建立时的初始化逻辑
        // 例如：连接数统计、IP白名单检查等
    }

    /**
     * 设备注册成功时的处理
     * 在设备鉴权通过并注册到SessionManager后触发
     *
     * @param session 已注册的会话
     */
    public void sessionRegistered(Session session) {
        String clientId = session.getClientId();
        log.info("设备注册成功: clientId={}, address={}", clientId, session.getRemoteAddressStr());

        // 在这里可以添加设备注册成功后的业务逻辑
        // 例如：
        // 1. 更新设备在线状态
        // 2. 推送离线期间的消息
        // 3. 同步设备参数
        // 4. 记录登录日志

        handleDeviceOnline(session);
    }

    /**
     * 设备连接断开时的处理
     * 在设备主动断开或网络异常导致连接断开时触发
     *
     * @param session 被销毁的会话
     */
    public void sessionDestroyed(Session session) {
        String clientId = session.getClientId();
        log.info("设备连接断开: clientId={}, address={}", clientId, session.getRemoteAddressStr());

        // 在这里可以添加设备断开连接后的清理逻辑
        // 例如：
        // 1. 更新设备离线状态
        // 2. 清理相关缓存
        // 3. 记录离线日志
        // 4. 处理未完成的业务

        handleDeviceOffline(session);
    }

    /**
     * 处理设备上线业务逻辑
     */
    private void handleDeviceOnline(Session session) {
        String clientId = session.getClientId();

        // 异步处理，避免阻塞主流程
        CompletableFuture.runAsync(() -> {
            try {
                // TODO 告知设备平台上线

                log.info("设备上线处理完成: {}", clientId);
            } catch (Exception e) {
                log.error("设备上线处理异常: clientId={}", clientId, e);
            }
        });
    }

    /**
     * 处理设备离线业务逻辑
     */
    private void handleDeviceOffline(Session session) {
        String clientId = session.getClientId();

        // 异步处理，避免阻塞主流程
        CompletableFuture.runAsync(() -> {
            try {
                // TODO 告知设备平台离线

                log.info("设备离线处理完成: {}", clientId);
            } catch (Exception e) {
                log.error("设备离线处理异常: clientId={}", clientId, e);
            }
        });
    }
}
