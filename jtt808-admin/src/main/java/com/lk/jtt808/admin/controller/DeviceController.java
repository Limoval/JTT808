package com.lk.jtt808.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lk.jtt808.admin.feign.DeviceFeignClient;
import com.lk.jtt808.admin.service.DeviceService;
import com.lk.jtt808.admin.service.LocationService;
import com.lk.jtt808.common.dto.ApiResponse;
import com.lk.jtt808.common.dto.CommandDTO;
import com.lk.jtt808.common.dto.DeviceDTO;
import com.lk.jtt808.common.dto.LocationDTO;
import com.lk.jtt808.common.entity.Device;
import com.lk.jtt808.common.entity.LocationRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final LocationService locationService;
    private final DeviceFeignClient deviceFeignClient;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取设备列表
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getDeviceList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) Integer status) {

        Page<Device> devicePage = deviceService.listDevices(page, size, deviceId, status);

        List<DeviceDTO> devices = devicePage.getRecords().stream()
                .map(deviceService::toDTO)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", devices);
        result.put("total", devicePage.getTotal());
        result.put("page", devicePage.getCurrent());
        result.put("size", devicePage.getSize());

        return ApiResponse.success(result);
    }

    /**
     * 获取设备详情
     */
    @GetMapping("/{deviceId}")
    public ApiResponse<DeviceDTO> getDeviceDetail(@PathVariable String deviceId) {
        Device device = deviceService.getDeviceById(deviceId);
        if (device == null) {
            return ApiResponse.error("设备不存在");
        }
        return ApiResponse.success(deviceService.toDTO(device));
    }

    /**
     * 发送命令到设备
     */
    @PostMapping("/{deviceId}/command")
    public ApiResponse<Object> sendCommand(
            @PathVariable String deviceId,
            @RequestBody CommandDTO commandDTO) {

        commandDTO.setDeviceId(deviceId);
        log.info("发送命令到设备: deviceId={}, command={}", deviceId, commandDTO);

        try {
            return deviceFeignClient.sendCommand(deviceId, commandDTO);
        } catch (Exception e) {
            log.error("发送命令失败: deviceId={}", deviceId, e);
            return ApiResponse.error("发送命令失败: " + e.getMessage());
        }
    }

    /**
     * 获取设备实时位置
     */
    @GetMapping("/{deviceId}/location/latest")
    public ApiResponse<LocationDTO> getLatestLocation(@PathVariable String deviceId) {
        LocationRecord record = locationService.getLatestLocation(deviceId);
        if (record == null) {
            return ApiResponse.error("未找到位置信息");
        }
        return ApiResponse.success(locationService.toDTO(record));
    }

    /**
     * 获取设备历史轨迹
     */
    @GetMapping("/{deviceId}/location/history")
    public ApiResponse<List<LocationDTO>> getLocationHistory(
            @PathVariable String deviceId,
            @RequestParam String startTime,
            @RequestParam String endTime) {

        LocalDateTime start = LocalDateTime.parse(startTime, DATE_TIME_FORMATTER);
        LocalDateTime end = LocalDateTime.parse(endTime, DATE_TIME_FORMATTER);

        List<LocationRecord> records = locationService.getLocationHistory(deviceId, start, end);
        List<LocationDTO> dtoList = records.stream()
                .map(locationService::toDTO)
                .collect(Collectors.toList());

        return ApiResponse.success(dtoList);
    }

    /**
     * 检查设备是否在线
     */
    @GetMapping("/{deviceId}/online")
    public ApiResponse<Boolean> isDeviceOnline(@PathVariable String deviceId) {
        try {
            return deviceFeignClient.isDeviceOnline(deviceId);
        } catch (Exception e) {
            log.error("检查设备在线状态失败: deviceId={}", deviceId, e);
            return ApiResponse.success(false);
        }
    }
}
