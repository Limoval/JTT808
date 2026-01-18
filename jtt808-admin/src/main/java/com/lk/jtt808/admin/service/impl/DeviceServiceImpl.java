package com.lk.jtt808.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lk.jtt808.admin.mapper.DeviceMapper;
import com.lk.jtt808.admin.service.DeviceService;
import com.lk.jtt808.common.dto.DeviceDTO;
import com.lk.jtt808.common.entity.Device;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 设备服务实现
 */
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;

    @Override
    public Page<Device> listDevices(Integer page, Integer size, String deviceId, Integer status) {
        Page<Device> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(deviceId)) {
            wrapper.like(Device::getDeviceId, deviceId);
        }
        if (status != null) {
            wrapper.eq(Device::getStatus, status);
        }
        wrapper.orderByDesc(Device::getUpdateTime);

        return deviceMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Device getDeviceById(String deviceId) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getDeviceId, deviceId);
        return deviceMapper.selectOne(wrapper);
    }

    @Override
    public DeviceDTO toDTO(Device device) {
        if (device == null) {
            return null;
        }
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(device.getDeviceId());
        dto.setDeviceName(device.getDeviceName());
        dto.setStatus(device.getStatus());
        dto.setAuthCode(device.getAuthCode());
        return dto;
    }
}
