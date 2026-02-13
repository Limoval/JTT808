package com.lk.jtt808.device.handler.inbound;


import com.lk.jtt808.common.entity.TerminalRegister;
import com.lk.jtt808.device.repository.TerminalRegisterRepository;
import com.lk.jtt808.device.session.SessionManager;
import com.lk.jtt808.device.transport.TransportSession;
import com.lk.jtt808.device.util.AuthCodeGenerator;
import com.lk.jtt808.protocol.constant.JT808;
import com.lk.jtt808.protocol.entity.T0100;
import com.lk.jtt808.protocol.entity.T8100;
import com.lk.jtt808.protocol.entity.enums.SessionKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;

@Service
@Slf4j
public class RegisterHandler extends AbstractInboundHandler<T0100> {

    public static final String AUTH_SALT = "hoDhqz5q";

    private final TerminalRegisterRepository terminalRegisterRepository;

    public RegisterHandler(SessionManager sessionManager,
                           TerminalRegisterRepository terminalRegisterRepository) {
        super(sessionManager);
        this.terminalRegisterRepository = terminalRegisterRepository;
    }

    @Override
    public Integer handle(T0100 msg, TransportSession session) {
        log.info("收到终端注册: {}", msg);
        String clientId = msg.getClientId();

        // 查询数据库中是否已存在该设备注册记录
        TerminalRegister existing = terminalRegisterRepository.findByDeviceId(clientId);
        if (existing != null) {
            if (ObjectUtils.nullSafeEquals(existing.getLicensePlateNumber(), msg.getLicense())) {
                log.info("handleDeviceRegistration 车牌 {} 已被注册", msg.getLicense());
                reply(msg, session, T8100.AlreadyRegisteredVehicle);
                return T8100.AlreadyRegisteredVehicle;
            } else {
                log.info("handleDeviceRegistration clientId {} 已被注册", clientId);
                reply(msg, session, T8100.AlreadyRegisteredTerminal);
                return T8100.AlreadyRegisteredTerminal;
            }
        }

        // 保存注册信息到数据库
        TerminalRegister register = new TerminalRegister();
        register.setDeviceId(clientId);
        register.setProvinceId(String.valueOf(msg.getProvinceId()));
        register.setCityId(String.valueOf(msg.getCityId()));
        register.setLicensePlateColor(msg.getLicenseColor());
        register.setLicensePlateNumber(msg.getLicense());
        register.setRegisterTime(LocalDateTime.now());
        register.setResult(0);
        try {
            terminalRegisterRepository.save(register);
        } catch (Exception e) {
            log.error("保存终端注册信息失败: clientId={}", clientId, e);
        }

        session.register(msg);
        this.reply(msg, session, T8100.Success);

        log.info("设备注册成功: {}", clientId);
        return 0;
    }

    @Override
    public void reply(T0100 msg, TransportSession session, Integer code) {
        T8100 t8100 = new T8100();
        t8100.setResponseSerialNo(msg.getInboundSerialNo());
        t8100.setResultCode(code);
        t8100.setClientId(msg.getClientId());
        t8100.setMessageId(JT808.终端注册应答);
        if (Integer.valueOf(T8100.Success).equals(code)) {
            String authCode = AuthCodeGenerator.generateAuthCode(msg.getClientId(), AUTH_SALT);
            session.setAttribute(SessionKey.AUTH_CODE, authCode);
            t8100.setToken(authCode);
        }
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
