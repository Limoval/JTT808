package com.lk.jtt808.device.controller;

import com.lk.jtt808.common.dto.ApiResponse;
import com.lk.jtt808.common.dto.CommandDTO;
import com.lk.jtt808.device.session.Session;
import com.lk.jtt808.device.session.SessionManager;
import com.lk.jtt808.protocol.entity.JT808Message;
import com.lk.jtt808.protocol.entity.T8103;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * 设备内部API控制器
 * 提供给jtt808-admin通过Feign调用的接口
 */
@Slf4j
@RestController
@RequestMapping("/internal/device")
@RequiredArgsConstructor
public class DeviceInternalController {

    private final SessionManager sessionManager;

    /**
     * 发送命令到设备
     */
    @PostMapping("/{deviceId}/command")
    public ApiResponse<Object> sendCommand(
            @PathVariable String deviceId,
            @RequestBody CommandDTO command) {

        log.info("收到发送命令请求: deviceId={}, command={}", deviceId, command);

        Session session = sessionManager.getSession(deviceId);
        if (session == null || !session.isRegistered()) {
            log.warn("设备不在线: deviceId={}", deviceId);
            return ApiResponse.error("设备不在线");
        }

        try {
            JT808Message message = buildCommandMessage(command);
            if (message == null) {
                return ApiResponse.error("不支持的命令类型: " + command.getCommandType());
            }

            // 发送命令到设备
            session.sendNotification(message)
                    .doOnSuccess(v -> log.info("命令发送成功: deviceId={}, type={}", deviceId, command.getCommandType()))
                    .doOnError(e -> log.error("命令发送失败: deviceId={}", deviceId, e))
                    .subscribe();

            return ApiResponse.success("命令已发送");
        } catch (Exception e) {
            log.error("发送命令异常: deviceId={}", deviceId, e);
            return ApiResponse.error("发送命令失败: " + e.getMessage());
        }
    }

    /**
     * 检查设备是否在线
     */
    @GetMapping("/{deviceId}/online")
    public ApiResponse<Boolean> isDeviceOnline(@PathVariable String deviceId) {
        boolean online = sessionManager.isDeviceOnline(deviceId);
        log.debug("检查设备在线状态: deviceId={}, online={}", deviceId, online);
        return ApiResponse.success(online);
    }

    /**
     * 获取在线设备数量
     */
    @GetMapping("/online/count")
    public ApiResponse<Integer> getOnlineDeviceCount() {
        int count = sessionManager.getOnlineSessions().size();
        return ApiResponse.success(count);
    }

    /**
     * 根据CommandDTO构建JT808消息
     */
    private JT808Message buildCommandMessage(CommandDTO command) {
        Integer type = command.getCommandType();
        if (type == null) {
            return null;
        }

        return switch (type) {
            case 0x8103 -> buildSetParamMessage(command);
            // TODO: 后续可以添加更多命令类型
            // case 0x8104 -> buildQueryParamMessage(command);
            // case 0x8201 -> buildQueryLocationMessage(command);
            default -> null;
        };
    }

    /**
     * 构建设置终端参数命令 (0x8103)
     */
    private T8103 buildSetParamMessage(CommandDTO command) {
        T8103 message = new T8103();
        // TODO: 根据command.getParams()设置具体参数
        return message;
    }
}
