package com.lk.jtt808.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lk.jtt808.common.dto.DeviceDTO;
import com.lk.jtt808.common.entity.Device;

/**
 * 设备服务接口
 */
public interface DeviceService {

    /**
     * 分页查询设备列表
     */
    Page<Device> listDevices(Integer page, Integer size, String deviceId, Integer status);

    /**
     * 获取设备详情
     */
    Device getDeviceById(String deviceId);

    /**
     * 转换为DTO
     */
    DeviceDTO toDTO(Device device);
}
