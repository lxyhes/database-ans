package com.data.assistant.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Qwen3-Coder 服务
 * 提供代码生成、分析和优化功能
 * 结合本地CLI和云端API
 */
@Service
public class Qwen3CoderService {
    
    @Value("${dashscope.api.key:}")
    private String apiKey;
    
    private Generation gen;
    
    @PostConstruct
    public void init() {
        gen = new Generation();
    }
    
    /**
     * 检查系统是否已安装Qwen3-Coder CLI
     */
    public boolean isQwen3CoderInstalled() {
        try {
            Process process = Runtime.getRuntime().exec("qwen-code --version");
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            System.out.println("Qwen3-Coder CLI 未安装或不可用: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 使用Qwen3-Coder生成代码（优先使用云端API，回退到CLI）
     */
    public String generateCodeWithQwen3(String description) {
        // 首先尝试使用云端API
        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                String prompt = "请根据以下描述生成高质量的代码：\n\n" + description + "\n\n要求：\n1. 代码要有适当的注释\n2. 遏循最佳实践\n3. 包含必要的错误处理";
                
                List<Message> messages = new ArrayList<>();
                messages.add(Message.builder()
                        .role(Role.SYSTEM.getValue())
                        .content("你是一个专业的代码生成器，能够根据自然语言描述生成高质量、可维护的代码。")
                        .build());
                messages.add(Message.builder()
                        .role(Role.USER.getValue())
                        .content(prompt)
                        .build());
                
                GenerationParam param = GenerationParam.builder()
                        .apiKey(apiKey)
                        .model("qwen3-coder") // 使用Qwen3-Coder模型
                        .messages(messages)
                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                        .topP(0.8)
                        .enableSearch(true)
                        .build();
                
                GenerationResult result = gen.call(param);
                
                String generatedCode = result.getOutput().getChoices().get(0).getMessage().getContent();
                // 清理输出，移除可能的markdown格式
                generatedCode = generatedCode.replaceAll("```java\\s*", "").replaceAll("```", "").trim();
                
                return generatedCode;
                
            } catch (Exception e) {
                System.out.println("云端API调用失败，尝试使用本地CLI: " + e.getMessage());
            }
        }
        
        // 如果云端API不可用，尝试使用CLI
        if (isQwen3CoderInstalled()) {
            try {
                // 构建CLI命令
                String command = "qwen-code generate --prompt \"" + description + "\"";
                ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
                processBuilder.redirectErrorStream(true);
                
                Process process = processBuilder.start();
                
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }
                
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    return output.toString();
                } else {
                    return "Qwen3-Coder CLI 执行失败，退出码: " + exitCode + "\n输出: " + output.toString();
                }
            } catch (IOException | InterruptedException e) {
                return "执行Qwen3-Coder CLI时发生错误: " + e.getMessage();
            }
        }
        
        // 如果都没有，返回本地生成的示例
        return "// 未安装Qwen3-Coder CLI且未配置API密钥\n// 根据描述: " + description + "\n\n// 这里应该生成相应的代码";
    }
    
    /**
     * 使用Qwen3-Coder优化现有代码（优先使用云端API，回退到CLI）
     */
    public String optimizeCodeWithQwen3(String code, String requirements) {
        // 首先尝试使用云端API
        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                String prompt = "请根据以下要求优化代码：\n\n要求：" + requirements + "\n\n代码：\n" + code + "\n\n请提供优化后的代码，并解释所做的更改。";
                
                List<Message> messages = new ArrayList<>();
                messages.add(Message.builder()
                        .role(Role.SYSTEM.getValue())
                        .content("你是一个专业的代码优化器，能够分析现有代码并根据要求进行优化，提高性能、可读性和可维护性。")
                        .build());
                messages.add(Message.builder()
                        .role(Role.USER.getValue())
                        .content(prompt)
                        .build());
                
                GenerationParam param = GenerationParam.builder()
                        .apiKey(apiKey)
                        .model("qwen3-coder") // 使用Qwen3-Coder模型
                        .messages(messages)
                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                        .topP(0.8)
                        .enableSearch(true)
                        .build();
                
                GenerationResult result = gen.call(param);
                
                String optimizedCode = result.getOutput().getChoices().get(0).getMessage().getContent();
                // 清理输出，移除可能的markdown格式
                optimizedCode = optimizedCode.replaceAll("```java\\s*", "").replaceAll("```", "").trim();
                
                return optimizedCode;
                
            } catch (Exception e) {
                System.out.println("云端API调用失败，尝试使用本地CLI: " + e.getMessage());
            }
        }
        
        // 如果云端API不可用，尝试使用CLI
        if (isQwen3CoderInstalled()) {
            try {
                // 将将代码写入临时文件以便CLI处理
                String command = "echo \"" + code.replace("\"", "\\\"") + "\" | qwen-code optimize --requirements \"" + requirements + "\"";
                ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
                processBuilder.redirectErrorStream(true);
                
                Process process = processBuilder.start();
                
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }
                
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    return output.toString();
                } else {
                    return "Qwen3-Coder CLI 执行失败，退出码: " + exitCode + "\n输出: " + output.toString();
                }
            } catch (IOException | InterruptedException e) {
                return "执行Qwen3-Coder CLI时发生错误: " + e.getMessage();
            }
        }
        
        // 如果都没有，返回本地优化的示例
        return "// 未安装Qwen3-Coder CLI且未配置API密钥\n\n" + code + "\n\n// 优化建议：\n// 1. 性能优化\n// 2. 可读性改进\n// 3. 最佳实践应用";
    }
    
    /**
     * 获取Qwen3-Coder CLI帮助信息
     */
    public String getQwen3CoderHelp() {
        if (!isQwen3CoderInstalled()) {
            return "Qwen3-Coder CLI 未安装。请运行: npm install -g @qwen-code/qwen-code";
        }
        
        try {
            Process process = Runtime.getRuntime().exec("qwen-code --help");
            StringBuilder output = new StringBuilder();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return output.toString();
            } else {
                return "获取帮助信息失败，退出码: " + exitCode;
            }
        } catch (IOException | InterruptedException e) {
            return "执行Qwen3-Coder CLI时发生错误: " + e.getMessage();
        }
    }
    
    /**
     * 分析代码（使用云端API）
     */
    public String analyzeCodeWithQwen3(String code) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "API密钥未设置，使用本地分析逻辑。\n\n代码分析结果：\n- 代码长度：" + code.split("\n").length + " 行\n- 包含 " + countKeywords(code) + " 个关键字";
        }
        
        try {
            String prompt = "请分析以下代码，提供详细的解释和改进建议：\n\n" + code;
            
            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content("你是一个专业的代码分析器，能够理解各种编程语言并提供详细的分析和改进建议。")
                    .build());
            messages.add(Message.builder()
                    .role(Role.USER.getValue())
                    .content(prompt)
                    .build());
            
            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model("qwen3-coder") // 使用Qwen3-Coder模型
                    .messages(messages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .topP(0.8)
                    .enableSearch(true)
                    .build();
            
            GenerationResult result = gen.call(param);
            
            String analysis = result.getOutput().getChoices().get(0).getMessage().getContent();
            return analysis;
            
        } catch (Exception e) {
            System.out.println("Qwen Code分析失败: " + e.getMessage());
            return "API调用失败，使用本地分析逻辑。\n\n代码分析结果：\n- 代码长度：" + code.split("\n").length + " 行\n- 包含 " + countKeywords(code) + " 个关键字";
        }
    }
    
    /**
     * 修复代码（使用云端API）
     */
    public String fixCodeWithQwen3(String code, String errorDescription) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "// API密钥未设置，使用本地修复逻辑\n\n" + code + "\n\n// 修复建议：\n// 错误描述: " + errorDescription;
        }
        
        try {
            String prompt = "请根据错误描述修复代码中的问题：\n\n错误描述：" + errorDescription + "\n\n代码：\n" + code + "\n\n请提供修复后的代码。";
            
            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content("你是一个专业的代码修复器，能够识别代码中的错误并提供修复方案。")
                    .build());
            messages.add(Message.builder()
                    .role(Role.USER.getValue())
                    .content(prompt)
                    .build());
            
            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model("qwen3-coder") // 使用Qwen3-Coder模型
                    .messages(messages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .topP(0.8)
                    .enableSearch(true)
                    .build();
            
            GenerationResult result = gen.call(param);
            
            String fixedCode = result.getOutput().getChoices().get(0).getMessage().getContent();
            // 清理输出，移除可能的markdown格式
            fixedCode = fixedCode.replaceAll("```java\\s*", "").replaceAll("```", "").trim();
            
            return fixedCode;
            
        } catch (Exception e) {
            System.out.println("Qwen Code修复失败: " + e.getMessage());
            return "// API调用失败，使用本地修复逻辑\n\n" + code + "\n\n// 修复建议：\n// 错误描述: " + errorDescription;
        }
    }
    
    /**
     * 解释代码（使用云端API）
     */
    public String explainCodeWithQwen3(String code) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "API密钥未设置，使用本地解释逻辑。\n\n代码解释：\n- 这是一个包含 " + code.split("\n").length + " 行的代码片段";
        }
        
        try {
            String prompt = "请详细解释以下代码的功能和实现逻辑：\n\n" + code;
            
            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content("你是一个专业的代码解释器，能够详细解释代码的功能、逻辑和实现方式。")
                    .build());
            messages.add(Message.builder()
                    .role(Role.USER.getValue())
                    .content(prompt)
                    .build());
            
            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model("qwen3-coder") // 使用Qwen3-Coder模型
                    .messages(messages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .topP(0.8)
                    .enableSearch(true)
                    .build();
            
            GenerationResult result = gen.call(param);
            
            String explanation = result.getOutput().getChoices().get(0).getMessage().getContent();
            return explanation;
            
        } catch (Exception e) {
            System.out.println("Qwen Code解释失败: " + e.getMessage());
            return "API调用失败，使用本地解释逻辑。\n\n代码解释：\n- 这是一个包含 " + code.split("\n").length + " 行的代码片段";
        }
    }
    
    /**
     * 异步生成代码
     */
    public CompletableFuture<String> generateCodeWithQwen3Async(String description) {
        return CompletableFuture.supplyAsync(() -> generateCodeWithQwen3(description));
    }
    
    /**
     * 异步优化代码
     */
    public CompletableFuture<String> optimizeCodeWithQwen3Async(String code, String requirements) {
        return CompletableFuture.supplyAsync(() -> optimizeCodeWithQwen3(code, requirements));
    }
    
    /**
     * 异步分析代码
     */
    public CompletableFuture<String> analyzeCodeWithQwen3Async(String code) {
        return CompletableFuture.supplyAsync(() -> analyzeCodeWithQwen3(code));
    }
    
    /**
     * 异步修复代码
     */
    public CompletableFuture<String> fixCodeWithQwen3Async(String code, String errorDescription) {
        return CompletableFuture.supplyAsync(() -> fixCodeWithQwen3(code, errorDescription));
    }
    
    /**
     * 异步解释代码
     */
    public CompletableFuture<String> explainCodeWithQwen3Async(String code) {
        return CompletableFuture.supplyAsync(() -> explainCodeWithQwen3(code));
    }
    
    /**
     * 本地关键词计数逻辑（备用）
     */
    private int countKeywords(String code) {
        String[] keywords = {"if", "else", "for", "while", "return", "function", "class", "def", "import", "from", "public", "private", "protected", "static", "final"};
        int count = 0;
        String lowerCode = code.toLowerCase();
        
        for (String keyword : keywords) {
            int index = 0;
            while ((index = lowerCode.indexOf(keyword, index)) != -1) {
                count++;
                index += keyword.length();
            }
        }
        
        return count;
    }
}