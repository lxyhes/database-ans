package com.data.assistant.controller;

import com.data.assistant.service.Qwen3CoderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Qwen3-Coder 控制器
 * 提供Qwen3-Coder CLI功能的API接口
 */
@RestController
@RequestMapping("/api/qwen3coder")
@CrossOrigin(origins = "*")
public class Qwen3CoderController {
    
    @Autowired
    private Qwen3CoderService qwen3CoderService;
    
    /**
     * 检查Qwen3-Coder CLI是否已安装
     */
    @GetMapping("/installed")
    public String checkInstallation() {
        boolean installed = qwen3CoderService.isQwen3CoderInstalled();
        return installed ? "Qwen3-Coder CLI 已安装" : "Qwen3-Coder CLI 未安装";
    }
    
    /**
     * 使用Qwen3-Coder生成代码
     */
    @PostMapping("/generate")
    public String generateCode(@RequestBody QwenCodeController.CodeRequest request) {
        return qwen3CoderService.generateCodeWithQwen3(request.getDescription());
    }
    
    /**
     * 使用Qwen3-Coder优化代码
     */
    @PostMapping("/optimize")
    public String optimizeCode(@RequestBody QwenCodeController.CodeRequest request) {
        return qwen3CoderService.optimizeCodeWithQwen3(request.getCode(), request.getRequirements());
    }
    
    /**
     * 使用Qwen3-Coder分析代码
     */
    @PostMapping("/analyze")
    public String analyzeCode(@RequestBody QwenCodeController.CodeRequest request) {
        return qwen3CoderService.analyzeCodeWithQwen3(request.getCode());
    }
    
    /**
     * 使用Qwen3-Coder修复代码
     */
    @PostMapping("/fix")
    public String fixCode(@RequestBody QwenCodeController.CodeRequest request) {
        return qwen3CoderService.fixCodeWithQwen3(request.getCode(), request.getErrorDescription());
    }
    
    /**
     * 使用Qwen3-Coder解释代码
     */
    @PostMapping("/explain")
    public String explainCode(@RequestBody QwenCodeController.CodeRequest request) {
        return qwen3CoderService.explainCodeWithQwen3(request.getCode());
    }
    
    /**
     * 获取Qwen3-Coder帮助信息
     */
    @GetMapping("/help")
    public String getHelp() {
        return qwen3CoderService.getQwen3CoderHelp();
    }
    
    /**
     * 异步生成代码
     */
    @PostMapping("/generate-async")
    public String generateCodeAsync(@RequestBody QwenCodeController.CodeRequest request) {
        return qwen3CoderService.generateCodeWithQwen3Async(request.getDescription()).join();
    }
    
    /**
     * 异步优化代码
     */
    @PostMapping("/optimize-async")
    public String optimizeCodeAsync(@RequestBody QwenCodeController.CodeRequest request) {
        return qwen3CoderService.optimizeCodeWithQwen3Async(request.getCode(), request.getRequirements()).join();
    }
    
    /**
     * 异步分析代码
     */
    @PostMapping("/analyze-async")
    public String analyzeCodeAsync(@RequestBody QwenCodeController.CodeRequest request) {
        return qwen3CoderService.analyzeCodeWithQwen3Async(request.getCode()).join();
    }
    
    /**
     * 异步修复代码
     */
    @PostMapping("/fix-async")
    public String fixCodeAsync(@RequestBody QwenCodeController.CodeRequest request) {
        return qwen3CoderService.fixCodeWithQwen3Async(request.getCode(), request.getErrorDescription()).join();
    }
    
    /**
     * 异步解释代码
     */
    @PostMapping("/explain-async")
    public String explainCodeAsync(@RequestBody QwenCodeController.CodeRequest request) {
        return qwen3CoderService.explainCodeWithQwen3Async(request.getCode()).join();
    }
}