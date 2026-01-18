package com.lk.jtt808.common.dto;

import lombok.Data;

/**
 * 统一响应格式
 */
@Data
public class ApiResponse<T> {
    private static final int SUCCESS_CODE = 200;
    private static final int ERROR_CODE = 500;

    private int code;
    private String message;
    private T data;
    private long timestamp;

    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, "success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(SUCCESS_CODE, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(ERROR_CODE, message, null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }

    public boolean isSuccess() {
        return this.code == SUCCESS_CODE;
    }
}