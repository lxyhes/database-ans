package com.data.assistant.service.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 服务工厂
 * 管理所有 AI 提供商的服务实例
 */
@Component
public class AIServiceFactory {
    
    @Autowired
    private List<AIService> aiServices;
    
    private Map<String, AIService> serviceMap = new HashMap<>();
    
    @PostConstruct
    public void init() {
        for (AIService service : aiServices) {
            serviceMap.put(service.getProviderName().toLowerCase(), service);
        }
    }
    
    /**
     * 获取指定提供商的 AI 服务
     */
    public AIService getService(String provider) {
        AIService service = serviceMap.get(provider.toLowerCase());
        if (service == null) {
            throw new IllegalArgumentException("未知的 AI 提供商: " + provider);
        }
        return service;
    }
    
    /**
     * 获取默认的 AI 服务（优先 iFlow）
     */
    public AIService getDefaultService() {
        // 优先返回可用的 iFlow
        if (serviceMap.containsKey("iflow") && serviceMap.get("iflow").isAvailable()) {
            return serviceMap.get("iflow");
        }
        // 其次返回可用的 DashScope（真实 AI）
        if (serviceMap.containsKey("dashscope") && serviceMap.get("dashscope").isAvailable()) {
            return serviceMap.get("dashscope");
        }
        // 最后回退到 mock（总是可用）
        if (serviceMap.containsKey("mock")) {
            return serviceMap.get("mock");
        }
        // 否则返回第一个可用的
        for (AIService service : serviceMap.values()) {
            if (service.isAvailable()) {
                return service;
            }
        }
        // 最后返回第一个
        return serviceMap.values().iterator().next();
    }
    
    /**
     * 获取所有可用的提供商
     */
    public Map<String, Boolean> getAvailableProviders() {
        Map<String, Boolean> providers = new HashMap<>();
        for (Map.Entry<String, AIService> entry : serviceMap.entrySet()) {
            providers.put(entry.getKey(), entry.getValue().isAvailable());
        }
        return providers;
    }
    
    /**
     * 检查提供商是否可用
     */
    public boolean isProviderAvailable(String provider) {
        AIService service = serviceMap.get(provider.toLowerCase());
        return service != null && service.isAvailable();
    }
}
