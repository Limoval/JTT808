# JTT808项目架构优化文档

## 优化概述

本次架构优化将原有的单模块项目重构为微服务架构，实现了设备接入服务和管理平台的分离，提高了系统的可扩展性和可维护性。

## 架构变化

### 原架构
```
JTT808 (单模块)
├── common (空模块)
└── service (所有代码)
```

### 新架构
```
JTT808 (父模块)
├── jtt808-protocol    # 协议定义模块
├── jtt808-common     # 公共组件模块
├── jtt808-device     # 设备接入服务 (808协议服务)
└── jtt808-admin      # 管理平台API服务
```

## 模块职责

### 1. jtt808-protocol
- **职责**: 定义JTT808协议相关的所有组件
- **内容**:
  - 协议注解 (@MessageType, @MessageField等)
  - 消息实体 (T0001, T0100, T0200等)
  - 编解码器 (Jtt808Encoder, Jtt808Decoder等)
  - 协议常量 (JT808, JT808Constant)
  - 字段转换器 (LatLonConverter, SpeedConverter等)

### 2. jtt808-common
- **职责**: 提供各模块共享的公共组件
- **内容**:
  - 数据传输对象 (DTO)
  - 数据库实体 (Entity)
  - 统一响应格式 (ApiResponse)
  - 枚举定义 (DeviceStatusEnum等)
  - 工具类
  - MyBatis映射文件

### 3. jtt808-device
- **职责**: 专门处理JTT808设备接入
- **端口**: 8081 (HTTP), 8082 (TCP)
- **功能**:
  - JTT808 TCP服务器 (Netty实现)
  - 设备注册、鉴权、心跳处理
  - 位置信息接收和存储
  - 设备会话管理
  - 提供设备管理相关的REST API

### 4. jtt808-admin
- **职责**: 提供管理后台的所有API
- **端口**: 8080
- **功能**:
  - 设备列表、详情查询
  - 实时位置查看
  - 历史轨迹查询
  - 报警信息管理
  - 统计数据接口
  - 通过OpenFeign调用jtt808-device服务

## 技术栈

### 基础技术
- Java 25
- Spring Boot 3.5.8
- Spring Cloud 2023.0.5
- Spring Cloud Alibaba 2023.0.1.2

### 关键组件
- **服务注册/配置中心**: Nacos (可选，支持开关)
- **数据库**: MySQL 8.0
- **ORM**: MyBatis Plus 3.5.9
- **连接池**: Druid 1.2.23
- **缓存**: Redis + Caffeine
- **网络通信**: Netty 4.2.5
- **服务调用**: OpenFeign
- **工具**: Hutool 5.8.40, Fastjson 1.2.83

## 数据库设计

已创建完整的数据库表结构，包括：
- `device` - 设备信息表
- `location_record` - 位置记录表（支持分区）
- `alarm_record` - 报警记录表
- `terminal_register` - 终端注册表
- `terminal_param` - 终端参数表
- `command_record` - 命令下发记录表

## 快速开始

### 1. 环境准备
```bash
# 安装JDK 25
# 安装Maven 3.6+
# 安装MySQL 8.0+
# 安装Redis
```

### 2. 初始化数据库
```bash
mysql -u root -p < sql/jtt808-schema.sql
```

### 3. 修改配置
- 编辑 `jtt808-device/src/main/resources/application.yml`
- 编辑 `jtt808-admin/src/main/resources/application.yml`
- 修改数据库和Redis连接信息

### 4. 编译项目
```bash
mvn clean compile
```

### 5. 启动服务
```bash
# 启动设备服务
cd jtt808-device
mvn spring-boot:run

# 启动管理服务（新开终端）
cd jtt808-admin
mvn spring-boot:run
```

### 6. 访问接口
- 设备接入: TCP://localhost:8082
- 管理API: http://localhost:8080/api
- Swagger文档: http://localhost:8080/swagger-ui.html

## 部署说明

### 开发环境
- 直接使用application.yml中的dev配置
- Nacos功能已关闭，使用本地配置

### 测试环境
- 使用test配置
- 连接测试环境的MySQL和Redis

### 生产环境
- 使用prod配置
- 开启Nacos服务发现和配置中心
- 使用Docker容器化部署
- 配置日志收集和监控

## 后续扩展

### 1. API网关 (可选)
- 使用Spring Cloud Gateway
- 统一入口、鉴权、限流
- 路由配置

### 2. 消息队列 (可选)
- 引入RabbitMQ或Kafka
- 处理高并发设备数据
- 异步处理报警信息

### 3. 监控告警
- 集成Prometheus + Grafana
- 添加ELK日志收集
- 实现服务健康检查

### 4. 前端管理界面
- 使用Vue 3 + Element Plus
- 实现设备管理、实时监控、数据分析
- WebSocket实现实时数据推送

## 注意事项

1. **依赖版本**: 确保JDK 25和Maven 3.6+
2. **Nacos可选**: 可根据需要开启/关闭Nacos功能
3. **数据库分区**: location_record表已按年份分区
4. **配置文件**: 生产环境敏感配置使用环境变量
5. **日志管理**: 日志文件自动切割，保留30天

## 联系方式

如有问题或建议，请联系项目维护人员。