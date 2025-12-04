package com.lk.jtt808.protocol.handler.inbound;


import com.lk.jtt808.protocol.entity.JT808Message;
import com.lk.jtt808.protocol.entity.T8001;
import com.lk.jtt808.protocol.handler.InboundHandler;
import com.lk.jtt808.protocol.session.Session;
import com.lk.jtt808.protocol.session.SessionManager;
import com.lk.jtt808.utils.JT808;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 请求处理抽象类
 * @param <T>
 */
@Slf4j
public abstract class AbstractInboundHandler<T extends JT808Message> implements InboundHandler<T> {


    protected final RedisTemplate<String, Object> redisTemplate;


    protected final SessionManager sessionManager;

    public AbstractInboundHandler(RedisTemplate<String, Object> redisTemplate, SessionManager sessionManager) {
        this.redisTemplate = redisTemplate;
        this.sessionManager = sessionManager;
    }

    protected static final String MQTT_MESSAGE_KEY = "mqtt:client_id:";


    /**
     * 根据信息组建topic发送mqtt信息到设备平台
     * @param clientId 设备id
     * @param metaData 源数据
     */
    protected void sendMessage(String clientId, String metaData) {

    }


    /**
     * 默认为平台通用应答,如有需求可自行实现
     * @param message
     */
    public void reply(T message, Session session, Integer code) {
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
