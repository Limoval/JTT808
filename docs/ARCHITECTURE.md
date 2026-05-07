# JTT808 车载终端通讯协议服务器 — 产品架构文档

> **协议标准**: JT/T 808-2013 / JT/T 808-2019
> **技术栈**: Java 21, Spring Boot 3.5.8, Netty 4.2.5, MyBatis Plus 3.5.9, Redis, MySQL 8.0
> **文档版本**: 1.0

---

## 第一章 系统架构概览

### 1.1 系统架构图

```
                              +-----------------+
                              |   管理前端 (Web) |
                              |   (Vue/React)   |
                              +--------+--------+
                                       |
                                       | REST API (8080)
                                       v
+----------------+           +-------------------+          +-------------------+
| Nacos 注册中心  |<----------| jtt808-admin      |          | Prometheus/Grafana|
| (可选)          |           | 管理后台服务       |          | 监控              |
+----------------+           +--------+----------+          +-------------------+
                                      |
                                      | OpenFeign (HTTP 8081)
                                      v
                             +-------------------+          +-------------------+
  车载终端 ====TCP 8082====> |                   |--------->| Redis             |
             ====UDP 8083====>  jtt808-device    |          | (实时数据缓存)     |
                             |  设备接入服务      |          +-------------------+
                             +--------+----------+
                                      |
                                      v
                             +-------------------+
                             | MySQL 8.0         |
                             | (持久化存储)       |
                             +-------------------+
```

### 1.2 模块依赖关系

```
jtt808-protocol  (纯协议层, 无 Spring 依赖)
    ^
    |
jtt808-common    (纯数据层, DTO/Entity/Enum)
    ^       ^
    |       |
    |    jtt808-device  (Netty TCP+UDP + Internal REST, 端口 8081/8082/8083)
    |       ^--- jtt808-protocol
    |
jtt808-admin  (Management REST API, 端口 8080, 通过 Feign 调用 device)
```

### 1.2.1 当前服务边界

当前阶段保持 admin 与 device 拆分，但不把它们设计成完全自治的微服务：

- `jtt808-device` 拥有设备运行态、Netty 会话、注册鉴权、命令下发、实时位置写入等核心写模型。
- `jtt808-admin` 作为管理查询侧，允许直接读取 MySQL 查询模型展示设备、位置、报警数据。
- admin 下发命令、查询在线态必须通过 device 内部 API，因为这些能力依赖 device 进程内会话。
- admin 不直接修改设备在线状态、命令状态、会话状态；这些状态由 device 统一维护。
- 如果后续需要多 device 实例或完全微服务化，应把运行态抽到 Redis/消息总线/专用查询 API，而不是继续扩大共享数据库写入面。

### 1.3 技术选型说明

| 组件 | 选型 | 理由 |
|------|------|------|
| 网络框架 | Netty 4.2.5 | 高性能 NIO，天然支持 TCP/UDP 双协议栈，适合大规模长连接 |
| Web 框架 | Spring Boot 3.5.8 | 生态成熟，自动配置，与 Netty 可在同 JVM 内共存 |
| 持久层 | MyBatis Plus 3.5.9 | 简化 CRUD，分页插件，代码生成器 |
| 缓存 | Redis + Caffeine | Redis 做分布式实时缓存，Caffeine 做进程内缓存（会话/分包/元数据） |
| 数据库 | MySQL 8.0 | 分区表支持，JSON 字段，窗口函数 |
| 服务间调用 | OpenFeign | admin → device 的命令下发通道，支持断路器降级 |
| 连接池 | Druid | SQL 监控，慢查询检测 |
| 响应式 | Reactor (Mono) | Session 层的请求-响应模式使用 Mono 异步处理 |

### 1.4 消息处理管道

```
传输层 (TCP 8082 / UDP 8083)
    ↓
FrameDecoder             → TCP: 提取 0x7E 帧, 反转义, 校验和验证
(TCP: Jtt808FrameDecoder)   UDP: DatagramPacket 直接反转义+校验 (无需分帧)
(UDP: UdpFrameDecoder)
    ↓
Jtt808MessageDecoder     → 解析消息头 (消息ID, 消息体长度, 终端号), 处理分包合并
    ↓
Jtt808MessageMapping     → 通过 MessageHandlerRegistry 查找消息类, 通过 MessageConverter 反射解析
    ↓
MessageProcessor         → 传输无关的消息处理: 响应匹配, 分发到 Handler (从 JTT808ServerHandler 提取)
    ↓
MessageHandlerDispatcher → Spring 自动发现 Handler, 按消息类型路由
    ↓
具体 Handler             → 业务逻辑处理, 通过 Repository 持久化
```

---

## 第二章 JTT808 协议消息完整清单

### 2.1 终端上行消息 (0x0001 — 0x0A00)

| 消息ID | 名称 | 2013 | 2019 | 实体类 | Handler | 优先级 |
|--------|------|:----:|:----:|--------|---------|:------:|
| 0x0001 | 终端通用应答 | Y | Y | T0001 ✅ | — (响应消息) | — |
| 0x0002 | 终端心跳 | Y | Y | T0002 ✅ | HeartbeatHandler ✅ | — |
| 0x0003 | 终端注销 | Y | Y | ❌ 待实现 | ❌ 待实现 | P1 |
| 0x0004 | 查询服务器时间 | — | Y | ❌ 待实现 | ❌ 待实现 | P2 |
| 0x0005 | 终端补传分包请求 | — | Y | ❌ 待实现 | ❌ 待实现 | P2 |
| 0x0100 | 终端注册 | Y | Y* | T0100 ✅ | RegisterHandler ✅ | — |
| 0x0102 | 终端鉴权 | Y | Y* | T0102 ✅ | AuthHandler ✅ | — |
| 0x0104 | 查询终端参数应答 | Y | Y | ❌ 待实现 | ❌ 待实现 | P1 |
| 0x0107 | 查询终端属性应答 | Y | Y | T0107 ✅ | — (响应消息) | — |
| 0x0108 | 终端升级结果通知 | Y | Y | ❌ 待实现 | ❌ 待实现 | P3 |
| 0x0200 | 位置信息汇报 | Y | Y | T0200 ✅ | LocationReportHandler ✅ | — |
| 0x0201 | 位置信息查询应答 | Y | Y | ❌ 待实现 | ❌ 待实现 | P1 |
| 0x0301 | 事件报告 | Y | 删除 | — | — | P4 |
| 0x0302 | 提问应答 | Y | 删除 | — | — | P4 |
| 0x0303 | 信息点播/取消 | Y | 删除 | — | — | P4 |
| 0x0500 | 车辆控制应答 | Y | Y | ❌ 待实现 | ❌ 待实现 | P2 |
| 0x0608 | 查询区域或线路数据应答 | — | Y | ❌ 待实现 | ❌ 待实现 | P2 |
| 0x0700 | 行驶记录数据上传 | Y | Y | ❌ 待实现 | ❌ 待实现 | P3 |
| 0x0701 | 电子运单上报 | Y | Y | ❌ 待实现 | ❌ 待实现 | P3 |
| 0x0702 | 驾驶员身份信息采集上报 | Y | Y* | ❌ 待实现 | ❌ 待实现 | P2 |
| 0x0704 | 定位数据批量上传 | Y | Y | ❌ 待实现 | ❌ 待实现 | P1 |
| 0x0705 | CAN 总线数据上传 | Y | Y | ❌ 待实现 | ❌ 待实现 | P3 |
| 0x0800 | 多媒体事件信息上传 | Y | Y | ❌ 待实现 | ❌ 待实现 | P3 |
| 0x0801 | 多媒体数据上传 | Y | Y | ❌ 待实现 | ❌ 待实现 | P3 |
| 0x0802 | 存储多媒体数据检索应答 | Y | Y | ❌ 待实现 | ❌ 待实现 | P3 |
| 0x0805 | 摄像头立即拍摄命令应答 | Y | Y | ❌ 待实现 | ❌ 待实现 | P3 |
| 0x0900 | 数据上行透传 | Y | Y | ❌ 待实现 | ❌ 待实现 | P2 |
| 0x0901 | 数据压缩上报 | Y | Y | ❌ 待实现 | ❌ 待实现 | P4 |
| 0x0A00 | 终端 RSA 公钥 | Y | Y | ❌ 待实现 | ❌ 待实现 | P4 |

> Y* = 2019 版有字段修改

### 2.2 平台下行消息 (0x8001 — 0x8A00)

| 消息ID | 名称 | 2013 | 2019 | 实体类 | 优先级 |
|--------|------|:----:|:----:|--------|:------:|
| 0x8001 | 平台通用应答 | Y | Y | T8001 ✅ | — |
| 0x8003 | 服务器补传分包请求 | Y | Y | ❌ 待实现 | P2 |
| 0x8004 | 查询服务器时间应答 | — | Y | ❌ 待实现 | P2 |
| 0x8100 | 终端注册应答 | Y | Y | T8100 ✅ | — |
| 0x8103 | 设置终端参数 | Y | Y | T8103 ✅ | — |
| 0x8104 | 查询终端参数 | Y | Y | ❌ 待实现 | P1 |
| 0x8105 | 终端控制 | Y | Y | ❌ 待实现 | P1 |
| 0x8106 | 查询指定终端参数 | Y | Y | ❌ 待实现 | P2 |
| 0x8107 | 查询终端属性 | Y | Y | T8107 ✅ | — |
| 0x8108 | 下发终端升级包 | Y | Y | ❌ 待实现 | P3 |
| 0x8201 | 位置信息查询 | Y | Y | ❌ 待实现 | P1 |
| 0x8202 | 临时位置跟踪控制 | Y | Y | ❌ 待实现 | P1 |
| 0x8203 | 人工确认报警消息 | Y | Y | ❌ 待实现 | P1 |
| 0x8204 | 服务器链路检测 | — | Y | ❌ 待实现 | P2 |
| 0x8300 | 文本信息下发 | Y | Y* | ❌ 待实现 | P1 |
| 0x8301 | 事件设置 | Y | 删除 | — | P4 |
| 0x8302 | 提问下发 | Y | 删除 | — | P4 |
| 0x8303 | 信息点播菜单设置 | Y | 删除 | — | P4 |
| 0x8304 | 信息服务 | Y | 删除 | — | P4 |
| 0x8400 | 电话回拨 | Y | Y | ❌ 待实现 | P2 |
| 0x8401 | 设置电话本 | Y | Y | ❌ 待实现 | P3 |
| 0x8500 | 车辆控制 | Y | Y* | ❌ 待实现 | P2 |
| 0x8600 | 设置圆形区域 | Y | Y* | ❌ 待实现 | P2 |
| 0x8601 | 删除圆形区域 | Y | Y | ❌ 待实现 | P2 |
| 0x8602 | 设置矩形区域 | Y | Y* | ❌ 待实现 | P2 |
| 0x8603 | 删除矩形区域 | Y | Y | ❌ 待实现 | P2 |
| 0x8604 | 设置多边形区域 | Y | Y* | ❌ 待实现 | P2 |
| 0x8605 | 删除多边形区域 | Y | Y | ❌ 待实现 | P2 |
| 0x8606 | 设置路线 | Y | Y | ❌ 待实现 | P2 |
| 0x8607 | 删除路线 | Y | Y | ❌ 待实现 | P2 |
| 0x8608 | 查询区域或线路数据 | — | Y | ❌ 待实现 | P2 |
| 0x8700 | 行驶记录仪数据采集命令 | Y | Y | ❌ 待实现 | P3 |
| 0x8701 | 行驶记录仪参数下传命令 | Y | Y | ❌ 待实现 | P3 |
| 0x8702 | 上报驾驶员身份信息请求 | Y | Y | ❌ 待实现 | P2 |
| 0x8800 | 多媒体数据上传应答 | Y | Y | ❌ 待实现 | P3 |
| 0x8801 | 摄像头立即拍摄命令 | Y | Y | ❌ 待实现 | P3 |
| 0x8802 | 存储多媒体数据检索 | Y | Y | ❌ 待实现 | P3 |
| 0x8803 | 存储多媒体数据上传 | Y | Y | ❌ 待实现 | P3 |
| 0x8804 | 录音开始命令 | Y | Y | ❌ 待实现 | P3 |
| 0x8805 | 单条存储多媒体数据检索上传命令 | Y | Y | ❌ 待实现 | P3 |
| 0x8900 | 数据下行透传 | Y | Y | ❌ 待实现 | P2 |
| 0x8A00 | 平台 RSA 公钥 | Y | Y | ❌ 待实现 | P4 |

### 2.3 优先级定义

| 级别 | 含义 | 数量 |
|------|------|------|
| P1 | 核心运营必须，直接影响设备管理和安全监控 | ~12 |
| P2 | 生产环境常用功能，提升平台能力 | ~20 |
| P3 | 特定行业/场景需求，按需实现 | ~15 |
| P4 | 2019 已删除的旧协议兼容，或极少使用 | ~8 |

### 2.4 完成度统计

- **已实现消息实体**: 10 个 (T0001, T0002, T0100, T0102, T0107, T0200, T8001, T8100, T8103, T8107)
- **已实现 Handler**: 4 个 (RegisterHandler, AuthHandler, HeartbeatHandler, LocationReportHandler)
- **待实现 P1**: ~12 个
- **待实现 P2**: ~20 个

---

## 第三章 传输层架构设计

### 3.1 问题分析

当前架构中传输层与业务层严重耦合:

| 问题 | 位置 | 影响 |
|------|------|------|
| Session 直接持有 Netty Channel | `Session.java` | 无法支持 UDP 无连接传输 |
| NettyServer 硬编码 TCP | `NettyServer.java` | 仅 NioServerSocketChannel |
| Handler 通过 Session 直接操作 Channel | `AuthHandler.java` | `session.getChannel().close()` |
| FrameDecoder 假设 TCP 流式分帧 | `Jtt808FrameDecoder.java` | UDP 数据报不需要 0x7E 分帧 |
| ServerHandler 绑定 Netty 生命周期 | `JTT808ServerHandler.java` | `channelActive/Inactive` 是 TCP 概念 |

### 3.2 策略模式重构

#### 核心接口定义

```
jtt808-device/src/main/java/com/lk/jtt808/device/transport/
├── TransportServer.java         # 传输服务器接口
├── TransportSession.java        # 传输会话接口 (业务层唯一依赖)
├── TransportType.java           # 枚举: TCP, UDP
├── MessageSender.java           # 消息发送策略接口
├── MessageProcessor.java        # 传输无关的消息处理 (从 JTT808ServerHandler 提取)
├── tcp/
│   ├── TcpTransportServer.java      # TCP ServerBootstrap 实现
│   ├── TcpChannelInitializer.java   # TCP pipeline 配置
│   ├── TcpServerHandler.java        # TCP 连接生命周期
│   └── TcpMessageSender.java       # channel.writeAndFlush()
└── udp/
    ├── UdpTransportServer.java      # UDP Bootstrap 实现
    ├── UdpChannelInitializer.java   # UDP pipeline 配置
    ├── UdpServerHandler.java        # 首包创建 Session, 超时清理
    ├── UdpMessageSender.java        # DatagramPacket 发送
    └── UdpOutbound.java             # UDP 出站编码封装
```

#### TransportServer 接口

```java
public interface TransportServer {
    void start() throws Exception;
    void stop();
    boolean isRunning();
    int getPort();
    TransportType getTransportType();
}
```

#### TransportSession 接口 (Handler 层唯一依赖)

```java
public interface TransportSession {
    // === 标识 ===
    String getSessionId();
    String getClientId();
    void setClientId(String clientId);
    TransportType getTransportType();

    // === 状态 ===
    boolean isRegistered();
    void setRegistered(boolean registered);
    boolean isAuthenticated();
    void setAuthenticated(boolean authenticated);
    boolean isActive();

    // === 消息发送 ===
    Mono<Void> sendMessage(JT808Message message);
    <T extends JT808Message> Mono<T> sendRequest(JT808Message request, Class<T> responseType, Duration timeout);

    // === 属性 ===
    void setAttribute(String key, Object value);
    <T> T getAttribute(String key, Class<T> type);

    // === 生命周期 ===
    void close();
    InetSocketAddress getRemoteAddress();
    int nextSerialNo();
}
```

#### MessageSender 接口 (传输策略)

```java
public interface MessageSender {
    Mono<Void> send(JT808Message message);
    boolean isWritable();
}
```

#### TCP vs UDP 关键差异

| 特性 | TCP | UDP |
|------|-----|-----|
| 连接管理 | channelActive 建立, channelInactive 销毁 | 首包建立, 超时销毁 (configurable) |
| 帧边界 | 需要 0x7E 分帧 (流式) | DatagramPacket 自带边界 |
| 空闲检测 | IdleStateHandler + ReadTimeoutHandler | 定时任务检查最后活跃时间 |
| 消息发送 | channel.writeAndFlush(msg) | channel.writeAndFlush(new DatagramPacket(buf, remoteAddr)) |
| 分包合并 | 支持 (有状态, 基于 Session) | 不建议 (无状态, 需要应用层确认) |
| 心跳 | 依赖 Netty 空闲事件 | 依赖定时检查 |

#### MessageProcessor (传输无关的消息处理)

从 `JTT808ServerHandler` 提取的核心逻辑:

```java
@Component
public class MessageProcessor {
    private final SessionManager sessionManager;
    private final MessageHandlerDispatcher dispatcher;

    /**
     * 处理解码后的消息 (TCP/UDP 共用)
     */
    public void processMessage(JT808Message message, TransportSession session) {
        // 1. 如果是响应消息, 匹配 Session 中等待的请求
        if (message instanceof JT808Response response) {
            session.completeRequest(response);
            return;
        }
        // 2. 分发到具体 Handler
        dispatcher.dispatch(message, session);
    }
}
```

### 3.3 UDP 帧解码器设计

TCP 使用现有的 `Jtt808FrameDecoder` (流式 0x7E 分帧)。

UDP 新建 `UdpFrameDecoder`:

```java
public class UdpFrameDecoder extends MessageToMessageDecoder<DatagramPacket> {
    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) {
        ByteBuf content = packet.content();
        // 1. 跳过首尾 0x7E (如果有)
        // 2. 反转义 (0x7D 0x01 → 0x7D, 0x7D 0x02 → 0x7E)
        // 3. 校验和验证
        // 4. 附带发送者地址信息, 传递给下游
    }
}
```

### 3.4 配置结构

```yaml
jtt808:
  transport:
    tcp:
      enabled: true
      port: 8082
      boss-threads: 1
      worker-threads: 0        # 0 = Netty 默认 (CPU*2)
      reader-idle-seconds: 180
      read-timeout-seconds: 300
    udp:
      enabled: false           # 按需开启
      port: 8083
      worker-threads: 0
      receive-buffer-size: 65536
      session-timeout-seconds: 300
```

---

## 第四章 消息处理器与 Repository 层设计

### 4.1 Handler 架构重构

#### 重构前 (紧耦合)

```java
public interface InboundHandler<T extends JT808Message> {
    Integer handle(T message, Session session);  // 依赖具体 Session 类
}

public abstract class AbstractInboundHandler<T> {
    @Autowired protected RedisTemplate redisTemplate;  // 直接依赖 Redis
    @Autowired protected SessionManager sessionManager;
}
```

#### 重构后 (解耦)

```java
public interface InboundHandler<T extends JT808Message> {
    Integer handle(T message, TransportSession session);  // 依赖接口
}

public abstract class AbstractInboundHandler<T> {
    // 无 RedisTemplate, 无 SessionManager
    // 由具体 Handler 按需注入 Repository
}
```

### 4.2 Repository 抽象层

#### 接口设计

```java
// === 设备仓储 ===
public interface DeviceRepository {
    Device findByDeviceId(String deviceId);
    void saveOrUpdate(Device device);
    void updateStatus(String deviceId, int status);
    void updateHeartbeat(String deviceId, LocalDateTime time);
    boolean existsByDeviceId(String deviceId);
}

// === 位置仓储 ===
public interface LocationRepository {
    void saveLocation(LocationRecord record);
    void saveLocationBatch(List<LocationRecord> records);
    LocationRecord getLatestLocation(String deviceId);
}

// === 报警仓储 ===
public interface AlarmRepository {
    void saveAlarm(AlarmRecord record);
}

// === 注册仓储 ===
public interface TerminalRegisterRepository {
    void save(TerminalRegister register);
    TerminalRegister findByDeviceId(String deviceId);
}

// === 命令记录仓储 ===
public interface CommandRecordRepository {
    void save(CommandRecord record);
    void updateResult(Long commandId, int result, LocalDateTime responseTime);
}
```

#### 实现层设计

每个 Repository 实现同时操作 Redis (实时缓存) 和 MySQL (持久化):

- **DeviceRepositoryImpl**: Redis 缓存设备状态/心跳 + MyBatis 持久化
- **LocationRepositoryImpl**: Redis 存最新位置 + 异步批量写入 MySQL
- **AlarmRepositoryImpl**: 直接写入 MySQL (报警不能丢) + Redis 通知
- **TerminalRegisterRepositoryImpl**: MySQL 为主
- **CommandRecordRepositoryImpl**: MySQL 为主

### 4.3 Handler 开发计划

#### P1 阶段 — 必须新增 (8 个)

| Handler/实体 | 消息ID | 说明 |
|-------------|--------|------|
| T0003 + TerminalLogoutHandler | 0x0003 | 清理 Session, 更新设备状态 |
| T0104 + QueryTerminalParamResponseHandler | 0x0104 | 终端参数应答, 持久化到 terminal_param |
| T0201 (JT808Response) | 0x0201 | 位置查询应答, 完成 sendRequest 的 Mono |
| T0704 + BatchLocationUploadHandler | 0x0704 | 批量位置/盲区补报, 批量写入 |
| T8104 (命令实体) | 0x8104 | 查询终端参数, 空消息体 |
| T8105 (命令实体) | 0x8105 | 终端控制 (关机/复位/恢复出厂等) |
| T8201 (命令实体) | 0x8201 | 位置信息查询, 空消息体 |
| T8203 (命令实体) | 0x8203 | 人工确认报警 |

#### P2 阶段 — 重要功能 (~12 个)

| 分类 | 消息 |
|------|------|
| 文本通信 | T8300 文本下发, T8400 电话回拨, T8202 临时跟踪 |
| 车辆控制 | T8500 车辆控制 + T0500 控制应答 |
| 电子围栏 | T8600/8601 圆形, T8602/8603 矩形, T8604/8605 多边形, T8606/8607 路线 |
| 透传 | T0900/T8900 上下行透传 |
| 驾驶员 | T0702 身份上报 + T8702 请求上报 |

#### P3 阶段 — 扩展功能

| 分类 | 消息 |
|------|------|
| 多媒体 | T0800/T0801/T0802/T0805 + T8800/T8801/T8802/T8803/T8804/T8805 |
| 行驶记录仪 | T0700 + T8700/T8701 |
| OTA 升级 | T8108 + T0108 |

---

## 第五章 命令下发体系设计

### 5.1 当前问题

1. `DeviceInternalController.buildCommandMessage()` 仅支持 0x8103，使用 `sendNotification` 无响应确认
2. `CommandService` 只有 `getClientParameter()` 一个方法
3. 命令执行无记录持久化 (command_record 表未使用)
4. 无命令执行状态追踪

### 5.2 重构后的命令下发链路

```
Admin (8080)                           Device (8081/8082)
+-----------------+                    +-----------------------+
| DeviceController|    Feign           | DeviceInternal        |
| /api/device/    | ----------------→ | Controller            |
| {id}/command    |                    | /internal/device/     |
+-----------------+                    +----------+------------+
                                                  |
                                       +----------v------------+
                                       | CommandService        |
                                       | (统一命令管理)         |
                                       +----------+------------+
                                                  |
                                       +----------v------------+
                                       | TransportSession      |
                                       | .sendRequest()        | ← 需要终端应答
                                       | .sendMessage()        | ← 单向通知
                                       +----------+------------+
                                                  |
                                       +----------v------------+
                                       | CommandRecordRepo     |
                                       | .save() / .update()   |
                                       +-----------------------+
```

### 5.3 CommandService 接口设计

```java
public interface CommandService {

    // === 参数管理 ===
    CommandResult queryTerminalParams(String clientId);
    CommandResult querySpecificParams(String clientId, List<Long> paramIds);
    CommandResult setTerminalParams(String clientId, Map<Long, byte[]> params);

    // === 位置查询 ===
    CommandResult queryLocation(String clientId);
    CommandResult setTemporaryTracking(String clientId, int interval, int duration);

    // === 设备控制 ===
    CommandResult queryTerminalAttributes(String clientId);
    CommandResult controlTerminal(String clientId, int commandWord, String commandParam);

    // === 报警管理 ===
    CommandResult confirmAlarm(String clientId, int alarmSerialNo, long alarmType);

    // === 文本通信 ===
    CommandResult sendTextMessage(String clientId, int flag, String text);

    // === 车辆控制 ===
    CommandResult controlVehicle(String clientId, int controlFlag);

    // === 电子围栏 ===
    CommandResult setCircularRegion(String clientId, int action, Object regionData);
    CommandResult setRectangularRegion(String clientId, int action, Object regionData);
    CommandResult setPolygonRegion(String clientId, int action, Object regionData);
    CommandResult setRoute(String clientId, Object routeData);

    // === 透传 ===
    CommandResult sendTransparentData(String clientId, int type, byte[] data);
}
```

### 5.4 命令类型映射表

| commandType | 消息ID | 名称 | 应答消息 |
|-------------|--------|------|----------|
| SET_PARAMS | 0x8103 | 设置终端参数 | T0001 |
| QUERY_ALL_PARAMS | 0x8104 | 查询终端参数 | T0104 |
| TERMINAL_CONTROL | 0x8105 | 终端控制 | T0001 |
| QUERY_SPEC_PARAMS | 0x8106 | 查询指定参数 | T0104 |
| QUERY_ATTRIBUTES | 0x8107 | 查询终端属性 | T0107 |
| QUERY_LOCATION | 0x8201 | 位置信息查询 | T0201 |
| TEMP_TRACKING | 0x8202 | 临时位置跟踪 | T0001 |
| ALARM_ACK | 0x8203 | 人工确认报警 | T0001 |
| SEND_TEXT | 0x8300 | 文本信息下发 | T0001 |
| PHONE_CALLBACK | 0x8400 | 电话回拨 | T0001 |
| VEHICLE_CONTROL | 0x8500 | 车辆控制 | T0500 |
| SET_CIRCLE_REGION | 0x8600 | 设置圆形区域 | T0001 |
| DEL_CIRCLE_REGION | 0x8601 | 删除圆形区域 | T0001 |
| SET_RECT_REGION | 0x8602 | 设置矩形区域 | T0001 |
| DEL_RECT_REGION | 0x8603 | 删除矩形区域 | T0001 |
| SET_POLYGON_REGION | 0x8604 | 设置多边形区域 | T0001 |
| DEL_POLYGON_REGION | 0x8605 | 删除多边形区域 | T0001 |
| SET_ROUTE | 0x8606 | 设置路线 | T0001 |
| DEL_ROUTE | 0x8607 | 删除路线 | T0001 |
| TRANSPARENT_DATA | 0x8900 | 数据下行透传 | T0001 |

### 5.5 命令执行记录

每条命令执行时:
1. `command_record` 表 INSERT (状态=发送中)
2. 发送命令到终端
3. 收到应答 → UPDATE (响应码, 响应时间, 结果=成功/失败)
4. 超时 → UPDATE (结果=超时)

---

## 第六章 数据流与存储设计

### 6.1 位置数据完整流程

```
T0200 位置汇报
  │
  ├─→ [实时层] Redis Hash "device:location:latest:{clientId}"
  │   存储: lat, lng, speed, direction, altitude, alarmFlag, statusFlag, time
  │   用途: 实时位置查询, 地图展示
  │
  ├─→ [持久层] MySQL location_record (通过 LocationRepository 异步批量写入)
  │   策略: 攒批 → 每 N 条或每 T 秒批量 INSERT
  │
  └─→ [报警分支] 若 alarmFlag != 0
      ├─→ MySQL alarm_record (直接写入, 报警不能丢)
      └─→ Redis Pub/Sub "alarm:channel" (通知管理后台, 可选)
```

### 6.2 批量位置上传流程 (0x0704)

```
T0704 批量位置上传
  ├─→ 解析 N 条位置数据
  ├─→ 更新 Redis 最新位置 (取时间最大的一条)
  ├─→ 全部通过 LocationRepository.saveLocationBatch() 写入
  └─→ 逐条检查报警标志
```

### 6.3 设备注册完整流程

```
T0100 终端注册
  ├─→ DeviceRepository.findByDeviceId() (查 MySQL)
  │   若不存在: INSERT device + INSERT terminal_register
  │   若已存在: 校验车牌/终端唯一性
  ├─→ 生成鉴权码, 存入 Session 属性
  └─→ 回复 T8100

T0102 终端鉴权
  ├─→ 验证鉴权码
  ├─→ DeviceRepository.updateStatus(deviceId, ONLINE)
  └─→ session.setAuthenticated(true)
```

### 6.4 心跳数据流程

```
T0002 心跳
  ├─→ Redis "device:heartbeat:{clientId}" (5 分钟 TTL)
  ├─→ DeviceRepository.updateHeartbeat() (异步, 可降频: 每 5 次更新一次 DB)
  └─→ 回复 T8001
```

### 6.5 设备上下线通知

```
SessionListener.sessionRegistered()  → DeviceRepository.updateStatus(ONLINE)
SessionListener.sessionDestroyed()   → DeviceRepository.updateStatus(OFFLINE)
```

### 6.6 Redis 键设计规范

| 键模式 | 类型 | TTL | 用途 |
|--------|------|-----|------|
| `device:location:latest:{clientId}` | Hash | 无 | 设备最新位置 |
| `device:heartbeat:{clientId}` | String | 5min | 心跳时间 |
| `device:online:{clientId}` | String | 无 | 在线标记 |
| `device:register:{clientId}` | Hash | 永久 | 注册信息缓存 |
| `device:params:{clientId}` | Hash | 30min | 终端参数缓存 |
| `command:pending:{commandId}` | String | 5min | 等待应答的命令 |

### 6.7 新增数据库表

#### geo_fence (电子围栏)

```sql
CREATE TABLE geo_fence (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fence_name VARCHAR(100) NOT NULL COMMENT '围栏名称',
    fence_type TINYINT NOT NULL COMMENT '类型 1:圆形 2:矩形 3:多边形 4:路线',
    fence_data JSON NOT NULL COMMENT '围栏数据(JSON)',
    attributes INT DEFAULT 0 COMMENT '围栏属性(按位标志)',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    max_speed INT COMMENT '最高速度(km/h)',
    overspeed_duration INT COMMENT '超速持续时间(秒)',
    status TINYINT DEFAULT 1 COMMENT '0:禁用 1:启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (fence_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### device_fence_binding (设备围栏绑定)

```sql
CREATE TABLE device_fence_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id VARCHAR(20) NOT NULL,
    fence_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_device_fence (device_id, fence_id),
    INDEX idx_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### driver_info (驾驶员信息)

```sql
CREATE TABLE driver_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id VARCHAR(20) NOT NULL,
    driver_name VARCHAR(50) COMMENT '驾驶员姓名',
    id_card VARCHAR(20) COMMENT '身份证号',
    qualification_cert VARCHAR(40) COMMENT '从业资格证号',
    cert_issuer VARCHAR(100) COMMENT '发证机构',
    cert_valid_period DATE COMMENT '有效期',
    report_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_device (device_id),
    INDEX idx_report_time (report_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 第七章 API 接口设计

### 7.1 Admin REST API (端口 8080)

#### 设备管理

| 方法 | 路径 | 状态 | 说明 |
|------|------|:----:|------|
| GET | `/api/device/list` | ✅ | 设备列表分页 |
| GET | `/api/device/{deviceId}` | ✅ | 设备详情 |
| POST | `/api/device` | 待实现 | 手动添加设备 (白名单) |
| PUT | `/api/device/{deviceId}` | 待实现 | 更新设备信息 |
| DELETE | `/api/device/{deviceId}` | 待实现 | 删除设备 |
| GET | `/api/device/online/list` | 待实现 | 所有在线设备 |
| GET | `/api/device/statistics` | 待实现 | 统计 (总数/在线/离线/故障) |

#### 位置管理

| 方法 | 路径 | 状态 | 说明 |
|------|------|:----:|------|
| GET | `/api/device/{deviceId}/location/latest` | ✅ | 最新位置 |
| GET | `/api/device/{deviceId}/location/history` | ✅ | 历史轨迹 |
| POST | `/api/device/{deviceId}/location/query` | 待实现 | 主动查询 (下发 0x8201) |
| POST | `/api/device/{deviceId}/location/track` | 待实现 | 临时跟踪 (下发 0x8202) |

#### 命令下发

| 方法 | 路径 | 状态 | 说明 |
|------|------|:----:|------|
| POST | `/api/device/{deviceId}/command` | ✅ 需重构 | 通用命令下发 |
| POST | `/api/device/{deviceId}/command/text` | 待实现 | 文本信息下发 |
| POST | `/api/device/{deviceId}/command/control` | 待实现 | 终端控制 |
| POST | `/api/device/{deviceId}/command/params` | 待实现 | 设置终端参数 |
| GET | `/api/device/{deviceId}/command/params` | 待实现 | 查询终端参数 |
| GET | `/api/device/{deviceId}/command/attributes` | 待实现 | 查询终端属性 |
| GET | `/api/command/history` | 待实现 | 命令历史记录 |
| GET | `/api/command/{commandId}` | 待实现 | 命令执行结果 |

#### 报警管理

| 方法 | 路径 | 状态 | 说明 |
|------|------|:----:|------|
| GET | `/api/alarm/list` | ✅ | 报警列表分页 |
| GET | `/api/alarm/{id}` | ✅ | 报警详情 |
| POST | `/api/alarm/{id}/handle` | ✅ | 处理报警 |
| POST | `/api/alarm/batch-handle` | ✅ | 批量处理 |
| POST | `/api/alarm/{id}/confirm` | 待实现 | 确认报警并下发终端 (0x8203) |
| GET | `/api/alarm/statistics` | 待实现 | 报警统计 |
| GET | `/api/alarm/types` | 待实现 | 报警类型字典 |

#### 电子围栏 (全部待实现)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/fence/list` | 围栏列表 |
| POST | `/api/fence` | 创建围栏 |
| PUT | `/api/fence/{id}` | 更新围栏 |
| DELETE | `/api/fence/{id}` | 删除围栏 |
| POST | `/api/fence/{id}/bind` | 绑定设备 |
| POST | `/api/fence/{id}/unbind` | 解绑设备 |
| POST | `/api/fence/{id}/deploy` | 下发围栏到设备 |

#### 系统管理 (全部待实现)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/system/metrics` | 系统指标 |
| GET | `/api/system/health` | 健康检查 |
| POST | `/api/system/user/login` | 用户登录 (JWT) |
| GET | `/api/system/user/info` | 当前用户信息 |

### 7.2 Device Internal API (端口 8081)

| 方法 | 路径 | 状态 | 说明 |
|------|------|:----:|------|
| POST | `/internal/device/{deviceId}/command` | ✅ 需重构 | 通用命令 |
| GET | `/internal/device/{deviceId}/online` | ✅ | 在线状态 |
| GET | `/internal/device/online/count` | ✅ | 在线数量 |
| GET | `/internal/device/online/list` | 待实现 | 在线设备 ID 列表 |
| GET | `/internal/device/{deviceId}/session` | 待实现 | 会话详情 |
| POST | `/internal/device/{deviceId}/command/sync` | 待实现 | 同步命令 |
| POST | `/internal/device/{deviceId}/command/async` | 待实现 | 异步命令 |

---

## 第八章 分阶段开发路线图

### Phase 1: 核心补全 — 数据持久化闭环 + 传输层重构

**目标**: 数据全部落库, 传输层解耦支持 TCP/UDP, 可接入真实终端。

| 任务 | 说明 |
|------|------|
| 传输层策略模式重构 | TransportServer/TransportSession/MessageSender 接口 + TCP 实现 + UDP 实现 |
| Repository 抽象层 | 5 个接口 + 5 个实现, 替换 Handler 中的直接 Redis 操作 |
| Handler 层解耦 | 所有 Handler 改用 TransportSession + Repository |
| 数据持久化补全 | 位置→MySQL, 报警→MySQL, 注册→MySQL, 心跳→异步更新 device 表 |
| P1 消息实体 + Handler | T0003/T0104/T0201/T0704/T8104/T8105/T8201/T8203 |
| CommandService 重构 | 扩展命令接口, 支持同步/异步, 记录持久化 |
| Admin API 补全 | 设备 CRUD, 主动位置查询, 报警确认下发, 命令历史 |

**交付物**: 完整的设备接入流程, 数据全部落库, TCP+UDP 双协议栈, 基本命令下发可用。

### Phase 2: 功能扩展 — 围栏/文本/车控/驾驶员

| 任务 | 说明 |
|------|------|
| 文本通信 | T8300 文本下发 + T8400 电话回拨 + T8202 临时跟踪 |
| 电子围栏 | T8600-T8608 全套 + geo_fence 表 + Admin API |
| 车辆控制 | T8500 + T0500 + Admin API |
| 驾驶员管理 | T0702 + T8702 + driver_info 表 + Admin API |
| 透传通道 | T0900/T8900 |

### Phase 3: 质量与安全

| 任务 | 说明 |
|------|------|
| 单元测试 | 编解码器, 消息实体序列化/反序列化, Handler (Mock), Session |
| 集成测试 | 端到端协议测试, Feign 链路, DB 持久化 |
| Spring Security + JWT | Admin API 认证鉴权, RBAC |
| API 文档 | SpringDoc(OpenAPI) 注解, Swagger UI |

### Phase 4: 生产部署与监控

| 任务 | 说明 |
|------|------|
| 容器化 | Dockerfile + docker-compose (MySQL/Redis/Prometheus/Grafana) |
| 监控 | Micrometer 自定义指标 (在线数/消息吞吐/命令耗时), Grafana 面板 |
| 日志 | Logback 生产配置, 结构化 JSON 日志 |
| 性能调优 | EventLoop 线程数, MySQL 连接池, Redis Pipeline, 批量 INSERT |

### Phase 5: 高级功能 (按需)

| 任务 | 说明 |
|------|------|
| 多媒体 | T0800/T0801 + T8800-T8805 + 文件存储 (MinIO/OSS) |
| 行驶记录仪 | T0700 + T8700/T8701 |
| OTA 升级 | T8108 + T0108 + 固件版本管理 |
| 2019 兼容完善 | T0100/T0102 字段差异, 新增消息 (T0004/T8004/T8204) |
| 集群部署 | Nacos 服务发现, Session 同步, 负载均衡 |

---

## 附录 A: 2013 与 2019 协议版本差异

| 差异点 | 2013 | 2019 |
|--------|------|------|
| 消息头长度 | 12 字节 (6 字节 BCD 终端号) | 17 字节 (10 字节 BCD 终端号 + 版本号) |
| 消息体长度位数 | 10 位 (最大 1023) | 12 位 (最大 4095) |
| 版本标识位 | bit14=0 | bit14=1 |
| 终端注册 (0x0100) | producerId=5, model=20, terminalId=7 | producerId=5, model=30, terminalId=30 |
| 终端鉴权 (0x0102) | 仅鉴权码 | 鉴权码长度+鉴权码+IMEI+软件版本 |
| 文本下发 (0x8300) | 标志位含事件/广告等 | 简化为紧急/终端显示等 |
| 车辆控制 (0x8500) | WORD 控制标志 | WORD 控制标志 + WORD 控制扩展标志 |
| 区域设置 (0x860X) | 无夜间限速 | 增加夜间限速等字段 |
| 删除消息 | — | 0x0301/0x0302/0x0303/0x8301/0x8302/0x8303/0x8304 |
| 新增消息 | — | 0x0004/0x0005/0x0608/0x8004/0x8204/0x8608 |

## 附录 B: 报警标志位定义 (T0200.alarmFlag)

| Bit | 含义 |
|-----|------|
| 0 | 紧急报警 (触动报警开关后触发) |
| 1 | 超速报警 |
| 2 | 疲劳驾驶报警 |
| 3 | 危险预警 |
| 4 | GNSS 模块故障 |
| 5 | GNSS 天线未接或被剪断 |
| 6 | GNSS 天线短路 |
| 7 | 终端主电源欠压 |
| 8 | 终端主电源掉电 |
| 9 | 终端 LCD 或显示器故障 |
| 10 | TTS 模块故障 |
| 11 | 摄像头故障 |
| 12 | 道路运输证 IC 卡模块故障 (2019) |
| 13 | 超速预警 (2019) |
| 14 | 疲劳驾驶预警 (2019) |
| 15-17 | 保留 |
| 18 | 当天累计驾驶超时 |
| 19 | 超时停车 |
| 20 | 进出区域 |
| 21 | 进出路线 |
| 22 | 路段行驶时间不足/过长 |
| 23 | 路线偏离报警 |
| 24 | 车辆 VSS 故障 |
| 25 | 车辆油量异常 |
| 26 | 车辆被盗 |
| 27 | 车辆非法点火 |
| 28 | 车辆非法位移 |
| 29 | 碰撞预警 |
| 30 | 侧翻预警 |
| 31 | 非法开门报警 |

## 附录 C: 状态标志位定义 (T0200.statusFlag)

| Bit | 含义 |
|-----|------|
| 0 | 0:ACC 关; 1:ACC 开 |
| 1 | 0:未定位; 1:定位 |
| 2 | 0:北纬; 1:南纬 |
| 3 | 0:东经; 1:西经 |
| 4 | 0:运营状态; 1:停运状态 |
| 5 | 0:经纬度未经保密插件加密; 1:经过加密 |
| 6-7 | 保留 |
| 8-9 | 00:空车; 01:半载; 10:保留; 11:满载 (适用货车/客车) |
| 10 | 0:车辆油路正常; 1:车辆油路断开 |
| 11 | 0:车辆电路正常; 1:车辆电路断开 |
| 12 | 0:车门解锁; 1:车门加锁 |
| 13 | 0:前门关; 1:前门开 |
| 14 | 0:中门关; 1:中门开 |
| 15 | 0:后门关; 1:后门开 |
| 16 | 0:驾驶席门关; 1:驾驶席门开 |
| 17 | 0:自定义; 1:自定义 |
| 18 | 0:未使用 GPS; 1:使用 GPS |
| 19 | 0:未使用北斗; 1:使用北斗 |
| 20 | 0:未使用 GLONASS; 1:使用 GLONASS |
| 21 | 0:未使用 Galileo; 1:使用 Galileo |
| 22-31 | 保留 |
