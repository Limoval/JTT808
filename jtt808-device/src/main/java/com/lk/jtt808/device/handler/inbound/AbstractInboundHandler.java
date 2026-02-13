package com.lk.jtt808.device.handler.inbound;


import com.lk.jtt808.device.handler.InboundHandler;
import com.lk.jtt808.device.session.SessionManager;
import com.lk.jtt808.device.transport.TransportSession;
import com.lk.jtt808.protocol.constant.JT808;
import com.lk.jtt808.protocol.entity.JT808Message;
import com.lk.jtt808.protocol.entity.T8001;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

/**
 * 请求处理抽象类
 * 移除 RedisTemplate 依赖，各子类通过 Repository 接口访问数据
 */
@Slf4j
public abstract class AbstractInboundHandler<T extends JT808Message> implements InboundHandler<T> {

    protected final SessionManager sessionManager;

    public AbstractInboundHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * 默认为平台通用应答
     */
    public void reply(T message, TransportSession session, Integer code) {
        T8001 t8001 = new T8001();
        BeanUtils.copyProperties(message, t8001);
        t8001.setMessageId(JT808.平台通用应答);
        t8001.setResponseSerialNo(message.getInboundSerialNo());
        t8001.setResponseMessageId(message.getMessageId());
        t8001.setResultCode(code);
        log.info("发送通用响应T8001 = {}", t8001);

        session.sendNotification(t8001)
                .doOnSuccess(success -> log.debug("平台通用应答发送成功: clientId={}", t8001.getClientId()))
                .doOnError(error -> log.error("发送平台通用应答失败: clientId={}", t8001.getClientId(), error))
                .subscribe();
    }

    @Override
    public void reply(T message, Integer code) {
        this.reply(message, sessionManager.getSession(message.getClientId()), code);
    }
}
