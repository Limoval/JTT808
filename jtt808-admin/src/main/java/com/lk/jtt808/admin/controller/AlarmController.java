package com.lk.jtt808.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lk.jtt808.admin.service.AlarmService;
import com.lk.jtt808.common.dto.AlarmDTO;
import com.lk.jtt808.common.dto.ApiResponse;
import com.lk.jtt808.common.entity.AlarmRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 告警管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    /**
     * 获取告警列表
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getAlarmList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) Integer alarmType,
            @RequestParam(required = false) Boolean handled) {

        Page<AlarmRecord> alarmPage = alarmService.listAlarms(page, size, deviceId, alarmType, handled);

        List<AlarmDTO> alarms = alarmPage.getRecords().stream()
                .map(alarmService::toDTO)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", alarms);
        result.put("total", alarmPage.getTotal());
        result.put("page", alarmPage.getCurrent());
        result.put("size", alarmPage.getSize());

        return ApiResponse.success(result);
    }

    /**
     * 获取告警详情
     */
    @GetMapping("/{id}")
    public ApiResponse<AlarmDTO> getAlarmDetail(@PathVariable Long id) {
        AlarmRecord alarm = alarmService.getAlarmById(id);
        if (alarm == null) {
            return ApiResponse.error("告警记录不存在");
        }
        return ApiResponse.success(alarmService.toDTO(alarm));
    }

    /**
     * 处理告警
     */
    @PostMapping("/{id}/handle")
    public ApiResponse<Boolean> handleAlarm(
            @PathVariable Long id,
            @RequestParam String handleUser,
            @RequestParam(required = false) String handleRemark) {

        log.info("处理告警: id={}, handleUser={}", id, handleUser);

        boolean success = alarmService.handleAlarm(id, handleUser, handleRemark);
        if (success) {
            return ApiResponse.success("处理成功", true);
        } else {
            return ApiResponse.error("处理失败，告警记录不存在或已处理");
        }
    }

    /**
     * 批量处理告警
     */
    @PostMapping("/batch-handle")
    public ApiResponse<Map<String, Object>> batchHandleAlarm(
            @RequestBody List<Long> ids,
            @RequestParam String handleUser,
            @RequestParam(required = false) String handleRemark) {

        log.info("批量处理告警: ids={}, handleUser={}", ids, handleUser);

        int successCount = 0;
        int failCount = 0;

        for (Long id : ids) {
            boolean success = alarmService.handleAlarm(id, handleUser, handleRemark);
            if (success) {
                successCount++;
            } else {
                failCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);

        return ApiResponse.success(result);
    }

    /**
     * 获取未处理告警数量
     */
    @GetMapping("/unhandled/count")
    public ApiResponse<Long> getUnhandledCount(@RequestParam(required = false) String deviceId) {
        Page<AlarmRecord> page = alarmService.listAlarms(1, 1, deviceId, null, false);
        return ApiResponse.success(page.getTotal());
    }
}
