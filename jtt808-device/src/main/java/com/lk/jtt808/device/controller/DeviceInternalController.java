package com.lk.jtt808.device.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lk.jtt808.common.dto.ApiResponse;
import com.lk.jtt808.common.dto.CommandDTO;
import com.lk.jtt808.device.service.CommandService;
import com.lk.jtt808.device.session.SessionManager;
import com.lk.jtt808.protocol.entity.JT808Message;
import com.lk.jtt808.protocol.entity.T8103;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 设备内部API控制器
 * 提供给jtt808-admin通过Feign调用的接口
 */
@Slf4j
@RestController
@RequestMapping("/internal/device")
@RequiredArgsConstructor
public class DeviceInternalController {

    private final SessionManager sessionManager;
    private final CommandService commandService;

    /**
     * 发送命令到设备
     */
    @PostMapping("/{deviceId}/command")
    public ApiResponse<Object> sendCommand(
            @PathVariable String deviceId,
            @RequestBody CommandDTO command) {

        log.info("收到发送命令请求: deviceId={}, command={}", deviceId, command);

        try {
            JT808Message message = buildCommandMessage(command);
            if (message == null) {
                return ApiResponse.error("不支持的命令类型: " + command.getCommandType());
            }

            Duration timeout = command.getTimeout() != null && command.getTimeout() > 0
                    ? Duration.ofSeconds(command.getTimeout())
                    : Duration.ofSeconds(30);

            Long commandId = commandService.sendCommand(deviceId, message, com.lk.jtt808.protocol.entity.T0001.class, timeout);

            return ApiResponse.success("命令已下发", commandId);
        } catch (IllegalArgumentException e) {
            log.warn("发送命令参数错误: deviceId={}", deviceId, e);
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("发送命令异常: deviceId={}", deviceId, e);
            return ApiResponse.error("发送命令失败: " + e.getMessage());
        }
    }

    /**
     * 检查设备是否在线
     */
    @GetMapping("/{deviceId}/online")
    public ApiResponse<Boolean> isDeviceOnline(@PathVariable String deviceId) {
        boolean online = sessionManager.isDeviceOnline(deviceId);
        log.debug("检查设备在线状态: deviceId={}, online={}", deviceId, online);
        return ApiResponse.success(online);
    }

    /**
     * 获取在线设备数量
     */
    @GetMapping("/online/count")
    public ApiResponse<Integer> getOnlineDeviceCount() {
        int count = sessionManager.getOnlineSessions().size();
        return ApiResponse.success(count);
    }

    /**
     * 根据CommandDTO构建JT808消息
     */
    private JT808Message buildCommandMessage(CommandDTO command) {
        Integer type = command.getCommandType();
        if (type == null) {
            return null;
        }

        return switch (type) {
            case 0x8103 -> buildSetParamMessage(command);
            // TODO: 后续可以添加更多命令类型
            // case 0x8104 -> buildQueryParamMessage(command);
            // case 0x8201 -> buildQueryLocationMessage(command);
            default -> null;
        };
    }

    /**
     * 构建设置终端参数命令 (0x8103)
     * 支持从 CommandDTO.commandParams 解析参数列表
     * JSON 格式示例：
     * [{"paramId": 1, "valueHex": "0000003C"}, {"paramId": 2, "valueType": "WORD", "paramValue": 30}]
     */
    private T8103 buildSetParamMessage(CommandDTO command) {
        if (command.getCommandParams() == null || command.getCommandParams().isBlank()) {
            throw new IllegalArgumentException("8103命令参数不能为空");
        }

        T8103 message = new T8103();
        List<T8103.TerminalParameter> parameters = new ArrayList<>();

        try {
            JSONArray jsonArray = JSON.parseArray(command.getCommandParams());
            if (jsonArray == null || jsonArray.isEmpty()) {
                throw new IllegalArgumentException("8103命令参数必须是非空JSON数组");
            }
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                if (obj == null) {
                    throw new IllegalArgumentException("8103参数项必须是JSON对象: index=" + i);
                }
                Long paramId = obj.getLong("paramId");
                if (paramId == null) {
                    throw new IllegalArgumentException("8103参数项缺少paramId: index=" + i);
                }
                if (paramId < 0 || paramId > 0xFFFF_FFFFL) {
                    throw new IllegalArgumentException("8103参数paramId超出DWORD范围: index=" + i + ", paramId=" + paramId);
                }

                byte[] valueBytes = parseTerminalParamValue(obj, i);
                if (valueBytes.length > 0xFF) {
                    throw new IllegalArgumentException("8103参数值长度超过255字节: index=" + i);
                }

                T8103.TerminalParameter parameter = new T8103.TerminalParameter();
                parameter.setParameterId(paramId);
                parameter.setParameterLength(valueBytes.length);
                parameter.setParameterValue(valueBytes);
                parameters.add(parameter);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("解析8103命令参数失败: " + e.getMessage(), e);
        }

        if (parameters.isEmpty()) {
            throw new IllegalArgumentException("8103命令至少需要一个有效参数");
        }
        message.setParameterCount(parameters.size());
        message.setParameters(parameters);
        return message;
    }

    private byte[] parseTerminalParamValue(JSONObject obj, int index) {
        String valueHex = firstNonBlank(obj.getString("valueHex"), obj.getString("paramValueHex"));
        if (valueHex != null) {
            return parseHexBytes(valueHex, "8103参数valueHex非法: index=" + index);
        }

        String valueType = obj.getString("valueType");
        Object paramValue = obj.get("paramValue");
        if (valueType == null || valueType.isBlank() || paramValue == null) {
            throw new IllegalArgumentException(
                    "8103参数项必须提供valueHex，或同时提供valueType和paramValue: index=" + index);
        }

        return switch (valueType.toUpperCase(Locale.ROOT)) {
            case "BYTE" -> new byte[] {(byte) parseUnsignedLong(paramValue, 0xFFL, "BYTE", index)};
            case "WORD" -> toWordBytes(parseUnsignedLong(paramValue, 0xFFFFL, "WORD", index));
            case "DWORD" -> toDwordBytes(parseUnsignedLong(paramValue, 0xFFFF_FFFFL, "DWORD", index));
            case "BCD" -> parseBcdValue(paramValue, index);
            case "STRING" -> String.valueOf(paramValue).getBytes(Charset.forName("GBK"));
            case "HEX" -> parseHexBytes(String.valueOf(paramValue), "8103参数HEX值非法: index=" + index);
            default -> throw new IllegalArgumentException("不支持的8103参数valueType: " + valueType + ", index=" + index);
        };
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private long parseUnsignedLong(Object value, long max, String type, int index) {
        try {
            long parsed = Long.parseUnsignedLong(String.valueOf(value));
            if (parsed > max) {
                throw new IllegalArgumentException();
            }
            return parsed;
        } catch (Exception e) {
            throw new IllegalArgumentException("8103参数" + type + "取值非法: index=" + index + ", value=" + value);
        }
    }

    private byte[] toWordBytes(long value) {
        return new byte[] {
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
    }

    private byte[] toDwordBytes(long value) {
        return new byte[] {
                (byte) ((value >> 24) & 0xFF),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
    }

    private byte[] parseBcdValue(Object value, int index) {
        String digits = String.valueOf(value);
        if (!digits.matches("\\d+")) {
            throw new IllegalArgumentException("8103参数BCD值必须只包含数字: index=" + index);
        }
        if ((digits.length() & 1) == 1) {
            digits = "0" + digits;
        }
        return parseHexBytes(digits, "8103参数BCD值非法: index=" + index);
    }

    private byte[] parseHexBytes(String rawHex, String errorMessage) {
        String hex = rawHex.replaceAll("\\s+", "");
        if ((hex.length() & 1) == 1 || !hex.matches("[0-9a-fA-F]*")) {
            throw new IllegalArgumentException(errorMessage);
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int high = Character.digit(hex.charAt(i * 2), 16);
            int low = Character.digit(hex.charAt(i * 2 + 1), 16);
            bytes[i] = (byte) ((high << 4) | low);
        }
        return bytes;
    }
}
