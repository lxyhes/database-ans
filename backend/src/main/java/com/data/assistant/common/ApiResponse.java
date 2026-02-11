package com.data.assistant.common;

import java.util.Map;

/**
 * API 统一响应工具类
 */
public class ApiResponse {

    /**
     * 成功响应（带数据）
     */
    public static Map<String, Object> success(Object data) {
        return Map.of(
            "success", true,
            "data", data
        );
    }

    /**
     * 成功响应（无数据）
     */
    public static Map<String, Object> success() {
        return Map.of(
            "success", true
        );
    }

    /**
     * 成功响应（带消息）
     */
    public static Map<String, Object> success(String message) {
        return Map.of(
            "success", true,
            "message", message
        );
    }

    /**
     * 错误响应
     */
    public static Map<String, Object> error(String message) {
        return Map.of(
            "success", false,
            "message", message
        );
    }

    /**
     * 错误响应（带详细错误信息）
     */
    public static Map<String, Object> error(String message, Object details) {
        return Map.of(
            "success", false,
            "message", message,
            "details", details
        );
    }
}
