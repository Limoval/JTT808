package com.lk.jtt808.protocol.handler.inbound;


import com.lk.jtt808.protocol.entity.T0100;
import com.lk.jtt808.protocol.entity.T8100;
import com.lk.jtt808.protocol.entity.enums.SessionKey;
import com.lk.jtt808.protocol.session.Session;
import com.lk.jtt808.protocol.session.SessionManager;
import com.lk.jtt808.utils.AuthCodeGenerator;
import com.lk.jtt808.utils.JT808;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

/**
 * @author: Limoval
 * time: 2025/6/9 17:26 周一
 * description:
 */
@Service
@Slf4j
public class RegisterHandler extends AbstractInboundHandler<T0100> {

    public static final String AUTH_SALT = "hoDhqz5q";

    public RegisterHandler(RedisTemplate<String, Object> redisTemplate, SessionManager sessionManager) {
        super(redisTemplate, sessionManager);
    }

    @Override
    public Integer handle(T0100 msg, Session session) {
        log.info("收到终端注册: {}", msg);
        String clientId = msg.getClientId();

        //查询redis里是否已经存在该clientId,如果存在则证明已经进行过clientId新增操作
        Boolean b = redisTemplate
                .opsForValue()
                .setIfAbsent(MQTT_MESSAGE_KEY + clientId, msg);
        if (Boolean.TRUE.equals(b)) {
            //新增产品设备

        } else {
            T0100 o = (T0100) redisTemplate.opsForValue().get(MQTT_MESSAGE_KEY + clientId);
            assert o != null;
            String license = o.getLicense();
            if (ObjectUtils.nullSafeEquals(license, msg.getLicense())) {
                //相等证明车牌已被注册
                log.info("handleDeviceRegistration 车牌 {} 已被注册", license);
                reply(msg, session, T8100.AlreadyRegisteredVehicle);
                return T8100.AlreadyRegisteredVehicle;
            } else {
                //不相等证明clientId被注册了
                log.info("handleDeviceRegistration clientId {} 已被注册", clientId);
                reply(msg, session, T8100.AlreadyRegisteredTerminal);
                return T8100.AlreadyRegisteredTerminal;
            }
        }
        session.register(msg);

        this.reply(msg, session, T8100.Success);

        log.info("设备注册成功: {}", clientId);

        return 0;
    }

    @Override
    public void reply(T0100 msg, Session session, Integer code) {
        // 构造响应消息（0x8100）
        T8100 t8100 = new T8100();
        t8100.setResponseSerialNo(msg.getInboundSerialNo());
        t8100.setResultCode(code);
        t8100.setClientId(msg.getClientId());
        t8100.setMessageId(JT808.终端注册应答);
        if (Integer.valueOf(T8100.Success).equals(code)) {
            String authCode = AuthCodeGenerator.generateAuthCode(msg.getClientId(), AUTH_SALT);
            //session.setAttribute(SessionKey.DEVICE, msg);
            session.setAttribute(SessionKey.AUTH_CODE, authCode);
            t8100.setToken(authCode);
        }
        //发送注册响应
        log.info("发送注册响应t8100 = {}", t8100);
        session.sendNotification(t8100)
                .doOnSuccess(success -> log.debug("注册应答发送成功: clientId={}", t8100.getClientId()))
                .doOnError(error -> log.error("发送注册应答失败: clientId={}", t8100.getClientId(), error))
                .subscribe();
    }

    @Override
    public Class<T0100> getHandleType() {
        return T0100.class;
    }


}
