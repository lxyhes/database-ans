package com.data.assistant.controller;

import com.data.assistant.service.QwenCodeService;
import com.data.assistant.service.QwenCodeSdkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Qwen Code 控制器
 * 提供代码分析、生成和优化功能
 */
@RestController
@RequestMapping("/api/qwen-code")
@CrossOrigin(origins = "*")
public class QwenCodeController {
    
    @Autowired
    private QwenCodeSdkService qwenCodeService;
    
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
     * 分析代码
     */
    @PostMapping("/analyze")
    public String analyzeCode(@RequestBody CodeRequest request) {
        return String.join("\n", qwenCodeService.analyzeCode(request.getCode()));
    }
    
    /**
     * 生成代码
     */
    @PostMapping("/generate")
    public String generateCode(@RequestBody CodeRequest request) {
        return String.join("\n", qwenCodeService.generateCode(request.getDescription()));
    }
    
    /**
     * 优化代码
     */
    @PostMapping("/optimize")
    public String optimizeCode(@RequestBody CodeRequest request) {
        String requirements = request.getRequirements();
        if (requirements == null || requirements.isEmpty()) {
            requirements = "提高性能和可读性";
        }
        return String.join("\n", qwenCodeService.optimizeCode(request.getCode(), requirements));
    }
    
    /**
     * 修复代码
     */
    @PostMapping("/fix")
    public String fixCode(@RequestBody CodeRequest request) {
        return String.join("\n", qwenCodeService.fixCode(request.getCode(), request.getErrorDescription()));
    }
    
    /**
     * 解释代码
     */
    @PostMapping("/explain")
    public String explainCode(@RequestBody CodeRequest request) {
        return String.join("\n", qwenCodeService.explainCode(request.getCode()));
    }
    
    /**
     * 异步分析代码
     */
    @PostMapping("/analyze-async")
    public String analyzeCodeAsync(@RequestBody CodeRequest request) {
        return String.join("\n", qwenCodeService.analyzeCode(request.getCode()));
    }
    
    /**
     * 异步生成代码
     */
    @PostMapping("/generate-async")
    public String generateCodeAsync(@RequestBody CodeRequest request) {
        return String.join("\n", qwenCodeService.generateCode(request.getDescription()));
    }
    
    /**
     * 异步优化代码
     */
    @PostMapping("/optimize-async")
    public String optimizeCodeAsync(@RequestBody CodeRequest request) {
        String requirements = request.getRequirements();
        if (requirements == null || requirements.isEmpty()) {
            requirements = "提高性能和可读性";
        }
        return String.join("\n", qwenCodeService.optimizeCode(request.getCode(), requirements));
    }
    
    /**
     * 异步修复代码
     */
    @PostMapping("/fix-async")
    public String fixCodeAsync(@RequestBody CodeRequest request) {
        return String.join("\n", qwenCodeService.fixCode(request.getCode(), request.getErrorDescription()));
    }
    
    /**
     * 异步解释代码
     */
    @PostMapping("/explain-async")
    public String explainCodeAsync(@RequestBody CodeRequest request) {
        return String.join("\n", qwenCodeService.explainCode(request.getCode()));
    }
}