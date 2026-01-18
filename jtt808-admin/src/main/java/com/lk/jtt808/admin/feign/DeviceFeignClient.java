package com.lk.jtt808.admin.feign;

import com.lk.jtt808.common.dto.ApiResponse;
import com.lk.jtt808.common.dto.CommandDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 设备服务Feign客户端
 * 用于调用jtt808-device服务发送命令
 * 包含断路器降级处理
 */
@FeignClient(
        name = "jtt808-device",
        url = "${feign.device.url:http://localhost:8081}",
        fallback = DeviceFeignClientFallback.class
)
public interface DeviceFeignClient {

    /**
     * 发送命令到设备
     */
    @PostMapping("/internal/device/{deviceId}/command")
    ApiResponse<Object> sendCommand(@PathVariable("deviceId") String deviceId,
                                    @RequestBody CommandDTO command);

    /**
     * 检查设备是否在线
     */
    @GetMapping("/internal/device/{deviceId}/online")
    ApiResponse<Boolean> isDeviceOnline(@PathVariable("deviceId") String deviceId);
}
