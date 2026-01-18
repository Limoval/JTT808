# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JTT808 protocol server implementation for vehicle telematics communication. Handles real-time communication with vehicle terminals supporting authentication, location reporting, heartbeat monitoring, and remote command execution.

**Tech Stack**: Java 21, Spring Boot 3.5.8, Netty 4.2.5, MyBatis Plus 3.5.9, Redis, MySQL 8.0, Spring Cloud (optional Nacos)

## Build & Run Commands

```bash
# Compile project
mvn clean compile

# Run tests
mvn test

# Package (skip tests)
mvn clean package -DskipTests

# Start device service (TCP + REST)
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
├── jtt808-device     # Device TCP server (port 8082) + Internal REST API (port 8081)
└── jtt808-admin      # Management REST API (port 8080)
```

**Module Dependencies**:
- jtt808-protocol → (standalone, no internal dependencies)
- jtt808-common → (standalone, no internal dependencies)
- jtt808-device → jtt808-protocol, jtt808-common
- jtt808-admin → jtt808-common (uses OpenFeign to call device service for commands)

## Architecture

### Message Processing Pipeline

```
TCP Connection (8082)
    ↓
Jtt808FrameDecoder     → Extract frames between 0x7E delimiters, unescape, verify checksum
    ↓
Jtt808MessageDecoder   → Parse header (msg ID, body length, client ID), handle subpackages
    ↓
Jtt808MessageMapping   → Lookup message class by ID, parse body using annotations
    ↓
JTT808ServerHandler    → Manage sessions, dispatch to handlers
    ↓
MessageHandlerDispatcher → Route to specific InboundHandler
    ↓
Handler (Register/Auth/Location/Heartbeat) → Business logic, send response
```

### Key Components

**Protocol Layer** (`jtt808-protocol/src/main/java/com/lk/jtt808/protocol/`):
- `annotation/` - @MessageType, @MessageField, MessageConverter, MessageHandlerRegistry
- `codec/` - Jtt808FrameDecoder, Jtt808MessageDecoder, Jtt808Encoder, Jtt808MessageMapping
- `entity/` - JT808Message base class, T0XXX/T8XXX message entities
- `converter/` - Field converters (LatLon, Speed, AdditionalInfo)
- `constant/JT808` - Contains all 88 message ID constants
- `util/` - BcdUtil, IntTool

**Device Service** (`jtt808-device/src/main/java/com/lk/jtt808/device/`):
- `netty/` - NettyServer, NettyChannelInitializer
- `handler/` - JTT808ServerHandler, MessageHandlerDispatcher
- `handler/inbound/` - RegisterHandler, AuthHandler, LocationReportHandler, HeartbeatHandler
- `session/` - Session, SessionManager, SessionListener
- `controller/` - DeviceInternalController (internal API for admin service)

**Admin Service** (`jtt808-admin/src/main/java/com/lk/jtt808/admin/`):
- `controller/` - DeviceController, AlarmController
- `service/` - DeviceService, LocationService, AlarmService
- `mapper/` - MyBatis Plus mappers for database access
- `feign/` - DeviceFeignClient for calling device service

**Common Module** (`jtt808-common/src/main/java/com/lk/jtt808/common/`):
- `entity/` - Device, LocationRecord, AlarmRecord, TerminalRegister, etc.
- `dto/` - ApiResponse, LocationDTO, AlarmDTO, CommandDTO, PageRequestDTO
- `enums/` - ColorEnum

**Data Types** (`DataType` enum): BYTE, WORD (2-byte), DWORD (4-byte), BCD, STRING, BYTES

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
    @MessageField(order = 1, type = DataType.WORD, desc = "Field description")
    private int fieldName;
}
```

2. Add message ID constant in `JT808` interface (`jtt808-protocol/.../constant/JT808.java`)

### Add Message Handler

1. Create handler in `jtt808-device/src/main/java/com/lk/jtt808/device/handler/inbound/`:
```java
@Service
public class MyHandler extends AbstractInboundHandler<T0XXX> {
    @Override
    public void handle(T0XXX message, Session session) {
        // Process message, send response
    }

    @Override
    public Class<T0XXX> getHandleType() {
        return T0XXX.class;
    }
}
```

Handlers are auto-discovered via `@Service` annotation.

## Configuration

**Ports**: Device TCP (8082), Device HTTP (8081), Admin HTTP (8080)

**Key Settings** (`application.yml`):
```yaml
jtt808:
  request-timeout: 30s
  heartbeat-interval: 60s
  auth-code-expire: 300s
```

**Database**: MySQL `jtt808` schema with tables: device, location_record, alarm_record, terminal_register, terminal_param, command_record

## Session Communication Patterns

```java
// One-way notification
session.sendNotification(message).subscribe();

// Request-response with timeout
session.sendRequest(message, T8001.class, Duration.ofSeconds(30))
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

The project has been reorganized into a clean multi-module architecture. Core protocol handling is complete for both 2013 and 2019 JTT808 versions.
