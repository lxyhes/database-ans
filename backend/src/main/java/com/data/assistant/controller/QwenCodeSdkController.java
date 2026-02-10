package com.data.assistant.controller;

import com.data.assistant.service.QwenCodeSdkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 基于文档实现的Qwen Code SDK控制器
 * 使用qwen-coder sdk.md文档中描述的API设计
 */
@RestController
@RequestMapping("/api/qwen-code-sdk")
@CrossOrigin(origins = "*")
public class QwenCodeSdkController {
    
    @Autowired
    private QwenCodeSdkService qwenCodeSdkService;
    
    /**
     * 代码请求实体类
     */
    public static class CodeRequest {
        private String code;
        private String description;
        private String requirements;
        private String errorDescription;
        
        // Getters and setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getRequirements() { return requirements; }
        public void setRequirements(String requirements) { this.requirements = requirements; }
        public String getErrorDescription() { return errorDescription; }
        public void setErrorDescription(String errorDescription) { this.errorDescription = errorDescription; }
    }
    
    /**
     * 简单查询
     */
    @PostMapping("/simple-query")
    public List<String> simpleQuery(@RequestBody CodeRequest request) {
        return qwenCodeSdkService.simpleQuery(request.getDescription());
    }
    
    /**
     * 带选项的查询
     */
    @PostMapping("/query-with-options")
    public List<String> queryWithOptions(@RequestBody CodeRequest request) {
        return qwenCodeSdkService.queryWithOptions(request.getDescription());
    }
    
    /**
     * 分析代码
     */
    @PostMapping("/analyze")
    public List<String> analyzeCode(@RequestBody CodeRequest request) {
        return qwenCodeSdkService.analyzeCode(request.getCode());
    }
    
    /**
     * 生成代码
     */
    @PostMapping("/generate")
    public List<String> generateCode(@RequestBody CodeRequest request) {
        return qwenCodeSdkService.generateCode(request.getDescription());
    }
    
    /**
     * 优化代码
     */
    @PostMapping("/optimize")
    public List<String> optimizeCode(@RequestBody CodeRequest request) {
        return qwenCodeSdkService.optimizeCode(request.getCode(), request.getRequirements());
    }
    
    /**
     * 修复代码
     */
    @PostMapping("/fix")
    public List<String> fixCode(@RequestBody CodeRequest request) {
        return qwenCodeSdkService.fixCode(request.getCode(), request.getErrorDescription());
    }
    
    /**
     * 解释代码
     */
    @PostMapping("/explain")
    public List<String> explainCode(@RequestBody CodeRequest request) {
        return qwenCodeSdkService.explainCode(request.getCode());
    }
}