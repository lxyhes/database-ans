package com.data.assistant.controller;

import com.data.assistant.service.ai.AIServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 提供系统状态监控和心跳检测
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private AIServiceFactory aiServiceFactory;

    /**
     * 心跳检测
     */
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("service", "data-analysis-assistant");
        return result;
    }

    /**
     * 系统状态检查
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new HashMap<>();
        
        // 基础状态
        result.put("status", "up");
        result.put("timestamp", LocalDateTime.now().toString());
        
        // AI 服务状态
        Map<String, Object> aiStatus = new HashMap<>();
        aiStatus.put("providers", aiServiceFactory.getAvailableProviders());
        result.put("aiServices", aiStatus);
        
        return result;
    }
}
