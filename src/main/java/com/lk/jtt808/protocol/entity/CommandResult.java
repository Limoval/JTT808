package com.lk.jtt808.protocol.entity;

import lombok.Data;

/**
 * 命令执行结果
 */
@Data
public class CommandResult {
    private boolean success;
    private String message;
    private Object data;
    private String clientId;
    private long commandId;
    private long executeTime;
    private String errorCode;
    
    public static CommandResult success(String clientId, Object data) {
        CommandResult result = new CommandResult();
        result.success = true;
        result.clientId = clientId;
        result.data = data;
        result.executeTime = System.currentTimeMillis();
        result.message = "执行成功";
        return result;
    }
    
    public static CommandResult failure(String clientId, String message) {
        CommandResult result = new CommandResult();
        result.success = false;
        result.clientId = clientId;
        result.message = message;
        result.executeTime = System.currentTimeMillis();
        return result;
    }
    
    public static CommandResult timeout(String clientId) {
        return failure(clientId, "设备响应超时");
    }
    
    public static CommandResult offline(String clientId) {
        return failure(clientId, "设备离线");
    }

}
