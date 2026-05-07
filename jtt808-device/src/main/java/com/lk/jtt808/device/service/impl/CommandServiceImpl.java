package com.lk.jtt808.device.service.impl;


import com.lk.jtt808.common.entity.CommandRecord;
import com.lk.jtt808.common.enums.CommandStatusEnum;
import com.lk.jtt808.device.repository.CommandRecordRepository;
import com.lk.jtt808.device.session.Session;
import com.lk.jtt808.device.session.SessionManager;
import com.lk.jtt808.device.service.CommandService;
import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.entity.JT808Message;
import com.lk.jtt808.protocol.entity.JT808Response;
import com.lk.jtt808.protocol.entity.base.AbstractGenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class CommandServiceImpl implements CommandService {

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private CommandRecordRepository commandRecordRepository;

    @Override
    public Long sendCommand(String deviceId, JT808Message message, Class<? extends JT808Response> responseClass, Duration timeout) {
        Session session = sessionManager.getSession(deviceId);
        if (session == null || !session.isRegistered() || !session.isAuthenticated()) {
            throw new IllegalArgumentException("设备不在线或未鉴权: " + deviceId);
        }

        // 确保 messageId 已设置（如果为0，从注解获取）
        if (message.getMessageId() == 0) {
            MessageType annotation = message.getClass().getAnnotation(MessageType.class);
            if (annotation != null) {
                message.setMessageId(annotation.value());
            }
        }
        if (message.getMessageId() == 0) {
            throw new IllegalArgumentException("命令消息缺少messageId: " + message.getClass().getName());
        }

        AtomicReference<Long> commandIdRef = new AtomicReference<>();
        Mono<? extends JT808Response> responseMono = session.sendRequest(message, responseClass, timeout, () -> {
            Long callbackCommandId = commandIdRef.get();
            if (callbackCommandId == null) {
                log.error("命令 SENT 回调发生在记录保存前: deviceId={}, messageId=0x{}",
                        deviceId, Integer.toHexString(message.getMessageId()));
                return;
            }
            try {
                CommandRecord sentRecord = new CommandRecord();
                sentRecord.setId(callbackCommandId);
                sentRecord.setStatus(CommandStatusEnum.SENT.getCode());
                commandRecordRepository.updateCommand(sentRecord);
                log.debug("命令状态更新为 SENT: commandId={}", callbackCommandId);
            } catch (Exception e) {
                log.error("更新命令 SENT 状态失败: commandId={}", callbackCommandId, e);
            }
        });

        // 1. 保存命令记录为 PENDING。sendRequest 会先同步执行出站拦截器，因此这里能拿到流水号。
        CommandRecord record = new CommandRecord();
        record.setDeviceId(deviceId);
        record.setCommandType(message.getMessageId());
        record.setCommandSerialNo(message.getOutboundSerialNo());
        record.setStatus(CommandStatusEnum.PENDING.getCode());
        record.setSendTime(LocalDateTime.now());
        commandRecordRepository.saveCommand(record);
        Long commandId = record.getId();
        commandIdRef.set(commandId);

        // 2. 发送命令，使用 onSendSuccess 回调更新为 SENT
        responseMono.subscribe(
                response -> {
                    // 3. 收到响应
                    try {
                        CommandStatusEnum finalStatus = CommandStatusEnum.SUCCESS;
                        Integer resultCode = null;
                        if (response instanceof AbstractGenericResponse resp) {
                            resultCode = resp.getResultCode();
                            if (resp.isFailure() || resp.isMessageError() || resp.isNotSupport()) {
                                finalStatus = CommandStatusEnum.FAILED;
                            }
                        }
                        CommandRecord resultRecord = new CommandRecord();
                        resultRecord.setId(commandId);
                        resultRecord.setStatus(finalStatus.getCode());
                        resultRecord.setResponseTime(LocalDateTime.now());
                        resultRecord.setResult(resultCode);
                        commandRecordRepository.updateCommand(resultRecord);
                        log.info("命令执行完成: commandId={}, status={}", commandId, finalStatus);
                    } catch (Exception e) {
                        log.error("更新命令响应状态失败: commandId={}", commandId, e);
                    }
                },
                error -> {
                    // 4. 超时或异常
                    try {
                        CommandStatusEnum errorStatus = error instanceof java.util.concurrent.TimeoutException
                                ? CommandStatusEnum.TIMEOUT : CommandStatusEnum.FAILED;
                        CommandRecord errorRecord = new CommandRecord();
                        errorRecord.setId(commandId);
                        errorRecord.setStatus(errorStatus.getCode());
                        errorRecord.setResponseTime(LocalDateTime.now());
                        commandRecordRepository.updateCommand(errorRecord);
                        log.warn("命令执行失败: commandId={}, status={}, error={}", commandId, errorStatus, error.getMessage());
                    } catch (Exception e) {
                        log.error("更新命令失败状态失败: commandId={}", commandId, e);
                    }
                }
        );

        return commandId;
    }

    @Override
    public CommandRecord getCommandStatus(Long commandId) {
        return commandRecordRepository.findById(commandId);
    }

}
