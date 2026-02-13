# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JTT808 protocol server implementation for vehicle telematics communication. Handles real-time communication with vehicle terminals supporting authentication, location reporting, heartbeat monitoring, and remote command execution.

**Tech Stack**: Java 21, Spring Boot 3.5.8, Netty 4.2.5, MyBatis Plus 3.5.9, Redis, MySQL 8.0, Spring Cloud (optional Nacos)

**Architecture Doc**: See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for full protocol message list, transport layer design, development roadmap, and API specifications.

## Build & Run Commands

```bash
# Compile project
mvn clean compile

# Run tests
mvn test

# Package (skip tests)
mvn clean package -DskipTests

# Start device service (TCP + UDP + REST)
cd jtt808-device && mvn spring-boot:run

# Start admin service (REST API)
cd jtt808-admin && mvn spring-boot:run

# Initialize database
mysql -u root -p < sql/jtt808-schema.sql
```

## Maven Multi-Module Structure

```
JTT808 (parent pom)
├── jtt808-protocol   # Protocol definitions, codecs, message entities, utilities
├── jtt808-common     # Shared DTOs, entities, enums
├── jtt808-device     # Device TCP/UDP server (port 8082/8083) + Internal REST API (port 8081)
└── jtt808-admin      # Management REST API (port 8080)
```

**Module Dependencies**:
- jtt808-protocol → (standalone, no internal dependencies)
- jtt808-common → (standalone, no internal dependencies)
- jtt808-device → jtt808-protocol, jtt808-common
- jtt808-admin → jtt808-common (uses OpenFeign to call device service for commands)

## Architecture

### Transport Layer (Strategy Pattern)

The transport layer uses a **strategy pattern** to support TCP and UDP:

```
transport/
├── TransportServer.java         # Interface: start()/stop()/isRunning()
├── TransportSession.java        # Interface: sendMessage()/close()/isActive() (Handler layer depends on this ONLY)
├── MessageSender.java           # Interface: send strategy
├── MessageProcessor.java        # Transport-agnostic message routing (extracted from JTT808ServerHandler)
├── TransportType.java           # Enum: TCP, UDP
├── tcp/
│   ├── TcpTransportServer.java      # NioServerSocketChannel bootstrap
│   ├── TcpChannelInitializer.java   # TCP pipeline (FrameDecoder → MessageDecoder → TcpServerHandler)
│   ├── TcpServerHandler.java        # TCP connection lifecycle (channelActive/Inactive)
│   └── TcpMessageSender.java       # channel.writeAndFlush()
└── udp/
    ├── UdpTransportServer.java      # NioDatagramChannel bootstrap
    ├── UdpChannelInitializer.java   # UDP pipeline (UdpFrameDecoder → MessageDecoder → UdpServerHandler)
    ├── UdpServerHandler.java        # First-packet session creation, timeout cleanup
    ├── UdpMessageSender.java        # DatagramPacket send
    └── UdpOutbound.java             # UDP outbound message wrapper
```

### Message Processing Pipeline

```
Transport Layer (TCP 8082 / UDP 8083)
    ↓
FrameDecoder                → TCP: Jtt808FrameDecoder (0x7E stream framing)
                              UDP: UdpFrameDecoder (DatagramPacket, no framing needed)
    ↓
Jtt808MessageDecoder        → Parse header (msg ID, body length, client ID), handle subpackages
    ↓
Jtt808MessageMapping        → Lookup message class by ID, parse body using annotations
    ↓
MessageProcessor            → Transport-agnostic: response matching, dispatch to Handler
    ↓
MessageHandlerDispatcher    → Spring auto-discover, route by message type
    ↓
Handler                     → Business logic via Repository abstraction
```

### Repository Abstraction Layer

Handlers use Repository interfaces (not RedisTemplate directly):

```
repository/
├── DeviceRepository.java              # findByDeviceId(), updateStatus(), updateHeartbeat()
├── LocationRepository.java            # saveLocation(), saveLocationToCache(), getLatestFromCache()
├── AlarmRepository.java               # saveAlarm()
├── TerminalRegisterRepository.java    # save(), findByDeviceId(), existsByDeviceId()
├── CommandRecordRepository.java       # save(), updateResult()
└── impl/
    ├── DeviceRepositoryImpl.java          # Redis cache + MySQL persistence
    ├── LocationRepositoryImpl.java        # Redis realtime + MySQL persistence
    ├── AlarmRepositoryImpl.java           # MySQL direct (alarms can't be lost)
    ├── TerminalRegisterRepositoryImpl.java
    └── CommandRecordRepositoryImpl.java
```

### Key Components

**Protocol Layer** (`jtt808-protocol/`):
- `annotation/` - @MessageType, @MessageField, simplified field annotations (@ByteField, @WordField, etc.), MessageConverter, MessageHandlerRegistry
- `codec/` - Jtt808FrameDecoder (TCP), UdpFrameDecoder (UDP), Jtt808MessageDecoder, Jtt808Encoder, Jtt808MessageMapping
- `entity/` - JT808Message base class, T0XXX/T8XXX message entities (10 implemented of 82 defined)
- `converter/` - FieldConverter interface + LatLonConverter, SpeedConverter, AdditionalInfoConverter
- `constant/JT808` - All 82 message ID constants
- `cache/` - MessageMetadataCache, MessageMetadata, FieldMetadata
- `util/` - BcdUtil, IntTool

**Device Service** (`jtt808-device/`):
- `transport/` - TransportServer/TransportSession/MessageSender interfaces + TCP/UDP implementations
- `handler/` - JTT808ServerHandler (TCP lifecycle), MessageProcessor, MessageHandlerDispatcher
- `handler/inbound/` - RegisterHandler, AuthHandler, LocationReportHandler, HeartbeatHandler
- `session/` - Session (implements TransportSession), SessionManager, SessionListener
- `repository/` - 5 Repository interfaces + 5 implementations (Redis + MySQL)
- `mapper/` - 5 MyBatis Plus mappers
- `controller/` - DeviceInternalController (internal API for admin service)

**Admin Service** (`jtt808-admin/`):
- `controller/` - DeviceController, AlarmController
- `service/` - DeviceService, LocationService, AlarmService
- `mapper/` - MyBatis Plus mappers
- `feign/` - DeviceFeignClient + Fallback

**Common Module** (`jtt808-common/`):
- `entity/` - Device, LocationRecord, AlarmRecord, TerminalRegister, TerminalParam, CommandRecord
- `dto/` - ApiResponse, LocationDTO, AlarmDTO, CommandDTO, DeviceDTO, PageRequestDTO
- `enums/` - ColorEnum, DeviceStatusEnum

**Data Types** (`DataType` enum): BYTE, WORD (2-byte), DWORD (4-byte), BCD, STRING, BYTES, LIST, OBJECT, CONDITIONAL, VARIABLE_LENGTH

### Message Naming Convention

- Terminal messages: `T0XXX` (0x0001-0x0FFF) - e.g., T0100 (Register), T0200 (Location)
- Platform messages: `T8XXX` (0x8001-0x8FFF) - e.g., T8001 (Response), T8100 (Register Response)

## Extending the Protocol

### Add New Message Type

1. Create class in `jtt808-protocol/src/main/java/com/lk/jtt808/protocol/entity/`:
```java
@MessageType(JT808.新消息ID)
@Data
public class T0XXX extends JT808Message {
    @WordField(desc = "Field description")
    private int fieldName;

    @DWordField(converter = LatLonConverter.class)
    private double latitude;
}
```

2. Add message ID constant in `JT808` interface if not already defined

### Add Message Handler

1. Create handler in `jtt808-device/src/main/java/com/lk/jtt808/device/handler/inbound/`:
```java
@Service
public class MyHandler extends AbstractInboundHandler<T0XXX> {

    private final SomeRepository someRepository;

    public MyHandler(SessionManager sessionManager, SomeRepository someRepository) {
        super(sessionManager);
        this.someRepository = someRepository;
    }

    @Override
    public Integer handle(T0XXX message, TransportSession session) {
        // Process message using repository (not RedisTemplate directly)
        someRepository.save(...);
        reply(message, session, T8001.Success);
        return 0;
    }

    @Override
    public Class<T0XXX> getHandleType() {
        return T0XXX.class;
    }
}
```

Handlers are auto-discovered via `@Service` annotation.

## Configuration

**Ports**: Device TCP (8082), Device UDP (8083), Device HTTP (8081), Admin HTTP (8080)

**Key Settings** (`application.yml`):
```yaml
jtt808:
  request-timeout: 30
  heartbeat-interval: 60
  auth-code-expire: 300
  transport:
    tcp:
      enabled: true
      port: 8082
    udp:
      enabled: false   # Enable when needed
      port: 8083
```

**Database**: MySQL `jtt808` schema with tables: device, location_record, alarm_record, terminal_register, terminal_param, command_record

## Session Communication Patterns

```java
// One-way notification (via TransportSession interface)
session.sendNotification(message).subscribe();

// Request-response with timeout
session.sendRequest(message, T0107.class, Duration.ofSeconds(30))
    .subscribe(response -> { }, error -> { });
```

## API Endpoints

### Admin Service (port 8080)

**Device Management**:
- `GET /api/device/list` - List devices with pagination
- `GET /api/device/{id}` - Get device details
- `GET /api/device/{deviceId}/location/latest` - Get latest location
- `GET /api/device/{deviceId}/location/history` - Get location history
- `POST /api/device/{deviceId}/command` - Send command to device

**Alarm Management**:
- `GET /api/alarm/list` - List alarms with pagination
- `GET /api/alarm/{id}` - Get alarm details
- `POST /api/alarm/{id}/handle` - Handle an alarm
- `POST /api/alarm/batch-handle` - Batch handle alarms

### Device Internal API (port 8081)

- `POST /internal/device/{deviceId}/command` - Send command (called by admin via Feign)
- `GET /internal/device/{deviceId}/online` - Check device online status
- `GET /internal/device/online/count` - Get online device count

## Project Status

The project has been reorganized into a decoupled multi-module architecture:

- **Transport layer**: Strategy pattern supporting TCP and UDP, fully decoupled from business logic
- **Repository layer**: 5 Repository interfaces abstract Redis + MySQL operations, Handlers never use RedisTemplate directly
- **Protocol layer**: Annotation-driven codec with metadata caching, 10 of 82 message entities implemented
- **Handler layer**: 4 handlers (Register/Auth/Heartbeat/Location) using TransportSession + Repository interfaces

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the complete development roadmap (Phase 1-5).
