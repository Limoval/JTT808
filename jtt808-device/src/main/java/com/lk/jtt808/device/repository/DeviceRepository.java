package com.lk.jtt808.device.repository;

import com.lk.jtt808.common.entity.Device;

/**
 * 设备仓储接口
 * 抽象设备数据的存储，实现层决定 Redis 缓存 + MySQL 持久化的策略
 */
public interface DeviceRepository {

    /**
     * 根据设备ID查询设备
     */
    Device findByDeviceId(String deviceId);

    /**
     * 更新设备状态（在线/离线）
     */
    void updateStatus(String deviceId, Integer status);

    /**
     * 更新心跳时间
     */
    void updateHeartbeat(String deviceId);

    /**
     * 保存或更新设备信息
     */
    void saveOrUpdate(Device device);
}
