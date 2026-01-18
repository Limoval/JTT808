package com.lk.jtt808.admin.feign;

import com.lk.jtt808.common.dto.ApiResponse;
import com.lk.jtt808.common.dto.CommandDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * DeviceFeignClient 断路器降级处理
 * 当设备服务不可用时，返回友好的错误响应
 */
@Component
@Slf4j
public class DeviceFeignClientFallback implements DeviceFeignClient {

    @Override
    public ApiResponse<Object> sendCommand(String deviceId, CommandDTO command) {
        log.warn("设备服务暂不可用，发送命令失败: deviceId={}, command={}", deviceId, command);
        return ApiResponse.error(503, "设备服务暂不可用，请稍后重试");
    }

    @Override
    public ApiResponse<Boolean> isDeviceOnline(String deviceId) {
        log.warn("设备服务暂不可用，无法获取设备状态: deviceId={}", deviceId);
        return ApiResponse.error(503, "无法获取设备状态，服务暂不可用");
    }
}
