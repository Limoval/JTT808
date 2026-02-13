package com.lk.jtt808.device.repository.impl;

import com.lk.jtt808.common.entity.LocationRecord;
import com.lk.jtt808.device.mapper.LocationRecordMapper;
import com.lk.jtt808.device.repository.LocationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Repository
@Slf4j
public class LocationRepositoryImpl implements LocationRepository {

    private static final String LOCATION_CACHE_PREFIX = "device:location:";
    private static final long CACHE_EXPIRE_MINUTES = 30;

    private final LocationRecordMapper locationRecordMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public LocationRepositoryImpl(LocationRecordMapper locationRecordMapper,
                                  RedisTemplate<String, Object> redisTemplate) {
        this.locationRecordMapper = locationRecordMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveLocation(LocationRecord record) {
        try {
            locationRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("保存位置记录到MySQL失败: deviceId={}", record.getDeviceId(), e);
        }
    }

    @Override
    public void saveLocationToCache(String clientId, Map<String, Object> locationData) {
        String cacheKey = LOCATION_CACHE_PREFIX + clientId;
        redisTemplate.opsForHash().putAll(cacheKey, locationData);
        redisTemplate.expire(cacheKey, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public Map<Object, Object> getLatestFromCache(String clientId) {
        String cacheKey = LOCATION_CACHE_PREFIX + clientId;
        return redisTemplate.opsForHash().entries(cacheKey);
    }
}
