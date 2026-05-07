-- ========================================
-- JTT808数据库表结构
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS jtt808 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE jtt808;

-- 设备表
CREATE TABLE device (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    device_id VARCHAR(20) UNIQUE NOT NULL COMMENT '终端手机号',
    device_name VARCHAR(100) COMMENT '设备名称',
    phone_number VARCHAR(20) UNIQUE COMMENT '手机号码',
    auth_code VARCHAR(8) COMMENT '鉴权码',
    device_type VARCHAR(20) DEFAULT '1' COMMENT '设备类型',
    manufacturer VARCHAR(50) COMMENT '制造商ID',
    model VARCHAR(50) COMMENT '设备型号',
    protocol_version VARCHAR(10) DEFAULT '1.0' COMMENT '协议版本号',
    status TINYINT DEFAULT 0 COMMENT '状态 0:离线 1:在线 2:休眠 3:故障',
    last_heartbeat DATETIME COMMENT '最后心跳时间',
    last_location_time DATETIME COMMENT '最后定位时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_device_id (device_id),
    INDEX idx_phone_number (phone_number),
    INDEX idx_status (status),
    INDEX idx_last_heartbeat (last_heartbeat)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备信息表';

-- 位置记录表
CREATE TABLE location_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    device_id VARCHAR(20) NOT NULL COMMENT '终端手机号',
    latitude DECIMAL(10,7) COMMENT '纬度',
    longitude DECIMAL(10,7) COMMENT '经度',
    altitude INT COMMENT '海拔高度(米)',
    speed INT COMMENT '速度(1/10km/h)',
    direction INT COMMENT '方向(0-359)',
    location_time DATETIME NOT NULL COMMENT '位置时间',
    alarm_flag BIGINT COMMENT '报警标志',
    status_flag BIGINT COMMENT '状态标志',
    altitude_flag TINYINT DEFAULT 1 COMMENT '高度定位是否有效 0:无效 1:有效',
    latitude_flag TINYINT DEFAULT 1 COMMENT '纬度定位是否有效 0:无效 1:有效',
    longitude_flag TINYINT DEFAULT 1 COMMENT '经度定位是否有效 0:无效 1:有效',
    speed_flag TINYINT DEFAULT 1 COMMENT '速度定位是否有效 0:无效 1:有效',
    direction_flag TINYINT DEFAULT 1 COMMENT '方向定位是否有效 0:无效 1:有效',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_device_time (device_id, location_time),
    INDEX idx_location_time (location_time),
    INDEX idx_device_id (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='位置记录表'
PARTITION BY RANGE (YEAR(location_time)) (
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 报警记录表
CREATE TABLE alarm_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    device_id VARCHAR(20) NOT NULL COMMENT '终端手机号',
    alarm_type INT NOT NULL COMMENT '报警类型',
    alarm_level TINYINT DEFAULT 2 COMMENT '报警级别 1:紧急 2:重要 3:一般',
    alarm_content VARCHAR(500) COMMENT '报警内容描述',
    latitude DECIMAL(10,7) COMMENT '报警位置纬度',
    longitude DECIMAL(10,7) COMMENT '报警位置经度',
    altitude INT COMMENT '报警位置海拔',
    alarm_time DATETIME NOT NULL COMMENT '报警时间',
    handled BOOLEAN DEFAULT FALSE COMMENT '是否已处理',
    handle_time DATETIME COMMENT '处理时间',
    handle_user VARCHAR(50) COMMENT '处理人',
    handle_remark VARCHAR(200) COMMENT '处理备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_device_alarm (device_id, alarm_time),
    INDEX idx_alarm_type (alarm_type),
    INDEX idx_handled (handled),
    INDEX idx_alarm_time (alarm_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警记录表';

-- 终端注册应答表
CREATE TABLE terminal_register (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    device_id VARCHAR(20) UNIQUE NOT NULL COMMENT '终端手机号',
    province_id VARCHAR(10) COMMENT '省域ID',
    city_id VARCHAR(10) COMMENT '市县域ID',
    manufacturer_id VARCHAR(10) COMMENT '制造商ID',
    terminal_model VARCHAR(20) COMMENT '终端型号',
    terminal_id VARCHAR(10) COMMENT '终端ID',
    license_plate_color TINYINT COMMENT '车牌颜色',
    license_plate_number VARCHAR(20) COMMENT '车牌号码',
    response_code TINYINT COMMENT '应答流水号',
    result TINYINT COMMENT '注册结果 0:成功 1:失败',
    verify_code VARCHAR(4) COMMENT '鉴权码',
    register_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    INDEX idx_device_id (device_id),
    INDEX idx_register_time (register_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='终端注册表';

-- 终端参数表
CREATE TABLE terminal_param (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    device_id VARCHAR(20) NOT NULL COMMENT '终端手机号',
    param_id INT NOT NULL COMMENT '参数ID',
    param_length INT COMMENT '参数长度',
    param_value VARCHAR(1000) COMMENT '参数值',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_device_param (device_id, param_id),
    INDEX idx_device_id (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='终端参数表';

-- 命令下发记录表
CREATE TABLE command_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    device_id VARCHAR(20) NOT NULL COMMENT '终端手机号',
    command_type INT NOT NULL COMMENT '命令类型',
    command_params TEXT COMMENT '命令参数',
    command_serial_no INT COMMENT '命令流水号',
    status TINYINT DEFAULT 0 COMMENT '状态 0:PENDING 1:SENT 2:SUCCESS 3:FAILED 4:TIMEOUT',
    send_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    response_time DATETIME COMMENT '响应时间',
    response_code TINYINT COMMENT '应答流水号',
    result TINYINT COMMENT '执行结果 0:成功 1:失败 2:消息有误 3:不支持 4:报警处理确认',
    INDEX idx_device_time (device_id, send_time),
    INDEX idx_command_type (command_type),
    INDEX idx_status (status),
    INDEX idx_result (result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='命令下发记录表';

-- 添加外键约束（可选，根据业务需要）
-- ALTER TABLE location_record ADD CONSTRAINT fk_location_device FOREIGN KEY (device_id) REFERENCES device(device_id);
-- ALTER TABLE alarm_record ADD CONSTRAINT fk_alarm_device FOREIGN KEY (device_id) REFERENCES device(device_id);
-- ALTER TABLE terminal_register ADD CONSTRAINT fk_register_device FOREIGN KEY (device_id) REFERENCES device(device_id);
-- ALTER TABLE terminal_param ADD CONSTRAINT fk_param_device FOREIGN KEY (device_id) REFERENCES device(device_id);
-- ALTER TABLE command_record ADD CONSTRAINT fk_command_device FOREIGN KEY (device_id) REFERENCES device(device_id);