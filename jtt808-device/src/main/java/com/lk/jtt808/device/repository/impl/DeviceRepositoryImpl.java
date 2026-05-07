package com.lk.jtt808.device.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lk.jtt808.common.entity.Device;
import com.lk.jtt808.device.mapper.DeviceMapper;
import com.lk.jtt808.device.repository.DeviceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Repository
@Slf4j
public class DeviceRepositoryImpl implements DeviceRepository {

    private static final String DEVICE_CACHE_PREFIX = "device:info:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    private final DeviceMapper deviceMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Executor taskExecutor;

    public DeviceRepositoryImpl(DeviceMapper deviceMapper,
                                RedisTemplate<String, Object> redisTemplate,
                                @Qualifier("taskExecutor") Executor taskExecutor) {
        this.deviceMapper = deviceMapper;
        this.redisTemplate = redisTemplate;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public Device findByDeviceId(String deviceId) {
        // 先查 Redis 缓存
        String cacheKey = DEVICE_CACHE_PREFIX + deviceId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof Device device) {
            return device;
        }

        // 缓存未命中，查 MySQL
        Device device = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>().eq(Device::getDeviceId, deviceId));

        if (device != null) {
            redisTemplate.opsForValue().set(cacheKey, device, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        return device;
    }

    @Override
    public void updateStatus(String deviceId, Integer status) {
        // 使缓存失效（下次查询时重建）
        String cacheKey = DEVICE_CACHE_PREFIX + deviceId;
        redisTemplate.delete(cacheKey);

        runStateUpdate("更新设备状态", deviceId, () ->
                deviceMapper.update(null, new LambdaUpdateWrapper<Device>()
                        .eq(Device::getDeviceId, deviceId)
                        .set(Device::getStatus, status)
                        .set(Device::getUpdateTime, LocalDateTime.now())));
    }

    @Override
    public void updateHeartbeat(String deviceId) {
        LocalDateTime now = LocalDateTime.now();

        // 使缓存失效（下次查询时重建）
        String cacheKey = DEVICE_CACHE_PREFIX + deviceId;
        redisTemplate.delete(cacheKey);

        runStateUpdate("更新心跳时间", deviceId, () ->
                deviceMapper.update(null, new LambdaUpdateWrapper<Device>()
                        .eq(Device::getDeviceId, deviceId)
                        .set(Device::getLastHeartbeat, now)
                        .set(Device::getUpdateTime, now)));
    }

    @Override
    public void saveOrUpdate(Device device) {
        Device existing = findByDeviceId(device.getDeviceId());
        if (existing != null) {
            device.setId(existing.getId());
            device.setUpdateTime(LocalDateTime.now());
            deviceMapper.updateById(device);
        } else {
            device.setCreateTime(LocalDateTime.now());
            device.setUpdateTime(LocalDateTime.now());
            deviceMapper.insert(device);
        }

        // 更新缓存
        String cacheKey = DEVICE_CACHE_PREFIX + device.getDeviceId();
        redisTemplate.opsForValue().set(cacheKey, device, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    private void runStateUpdate(String action, String deviceId, Runnable updateTask) {
        try {
            taskExecutor.execute(() -> {
                int maxAttempts = 3;
                for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                    try {
                        updateTask.run();
                        return;
                    } catch (Exception e) {
                        if (attempt == maxAttempts) {
                            log.error("{}失败且已达到最大重试次数: deviceId={}, attempts={}", action, deviceId, attempt, e);
                        } else {
                            log.warn("{}失败，准备重试: deviceId={}, attempt={}", action, deviceId, attempt, e);
                            sleepBeforeRetry(attempt);
                        }
                    }
                }
            });
        } catch (RuntimeException e) {
            log.error("{}任务提交失败: deviceId={}", action, deviceId, e);
        }
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(200L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
