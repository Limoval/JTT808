package com.lk.jtt808.device.repository;

import com.lk.jtt808.common.entity.LocationRecord;

import java.util.Map;

/**
 * 位置仓储接口
 * 支持 Redis 实时缓存和 MySQL 持久化
 */
public interface LocationRepository {

    /**
     * 保存位置记录到 MySQL
     */
    void saveLocation(LocationRecord record);

    /**
     * 保存最新位置到 Redis 缓存
     */
    void saveLocationToCache(String clientId, Map<String, Object> locationData);

    /**
     * 从 Redis 获取最新位置
     */
    Map<Object, Object> getLatestFromCache(String clientId);
}
