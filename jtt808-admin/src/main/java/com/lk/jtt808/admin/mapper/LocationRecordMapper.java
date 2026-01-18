package com.lk.jtt808.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lk.jtt808.common.entity.LocationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 位置记录Mapper
 */
@Mapper
public interface LocationRecordMapper extends BaseMapper<LocationRecord> {

    /**
     * 获取设备最新位置
     */
    @Select("SELECT * FROM location_record WHERE device_id = #{deviceId} ORDER BY location_time DESC LIMIT 1")
    LocationRecord selectLatestByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 查询历史轨迹
     */
    @Select("SELECT * FROM location_record WHERE device_id = #{deviceId} AND location_time BETWEEN #{startTime} AND #{endTime} ORDER BY location_time ASC")
    List<LocationRecord> selectHistory(@Param("deviceId") String deviceId,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);
}
