package com.data.assistant.service;

import com.alibaba.qwen.code.cli.QwenCodeCli;
import com.alibaba.qwen.code.cli.transport.TransportOptions;
import com.alibaba.qwen.code.cli.session.event.consumers.AssistantContentSimpleConsumers;
import com.alibaba.qwen.code.cli.protocol.data.*;
import com.alibaba.qwen.code.cli.protocol.data.behavior.Behavior.Operation;
import com.alibaba.qwen.code.cli.session.Session;
import com.alibaba.qwen.code.cli.utils.Timeout;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import com.alibaba.qwen.code.cli.protocol.data.AssistantContent.TextAssistantContent;
import com.alibaba.qwen.code.cli.protocol.data.AssistantContent.ThingkingAssistantContent;
import com.alibaba.qwen.code.cli.protocol.data.AssistantContent.ToolUseAssistantContent;
import com.alibaba.qwen.code.cli.protocol.data.AssistantContent.ToolResultAssistantContent;

/**
 * Qwen Code SDK服务包装类
 * 按照qwen-coder sdk.md文档中的API设计实现
 */
@Service
public class QwenCodeSdkService {
    
    /**
     * 简单查询方法 - 对应文档中的 QwenCodeCli.simpleQuery()
     */
    public List<String> simpleQuery(String prompt) {
        return QwenCodeCli.simpleQuery(prompt);
    }
    
    /**
     * 带传输选项的查询方法 - 对应文档中的高级用法
     */
    public List<String> queryWithOptions(String prompt) {
        TransportOptions options = new TransportOptions()
                .setModel("qwen3-coder")
                .setPermissionMode(PermissionMode.AUTO_EDIT)
                .setCwd("./")
                .setIncludePartialMessages(true)
                .setTurnTimeout(new Timeout(120L, TimeUnit.SECONDS))
                .setMessageTimeout(new Timeout(90L, TimeUnit.SECONDS));
        
        return QwenCodeCli.simpleQuery(prompt, options);
    }
    
    /**
     * 流式内容处理 - 对应文档中的流式示例
     */
    public void streamQuery(String prompt) {
        QwenCodeCli.simpleQuery(prompt,
                new TransportOptions().setMessageTimeout(new Timeout(10L, TimeUnit.SECONDS)), 
                new AssistantContentSimpleConsumers() {

                    @Override
                    public void onText(Session session, TextAssistantContent textAssistantContent) {
                        System.out.println("收到文本内容: " + textAssistantContent.getText());
                    }

                    @Override
                    public void onThinking(Session session, ThingkingAssistantContent thingkingAssistantContent) {
                        System.out.println("收到思考内容: " + thingkingAssistantContent.getThinking());
                    }

                    @Override
                    public void onToolUse(Session session, ToolUseAssistantContent toolUseContent) {
                        System.out.println("工具使用: " + toolUseContent + " 参数: " + toolUseContent.getInput());
                    }

                    @Override
                    public void onToolResult(Session session, ToolResultAssistantContent toolResultContent) {
                        System.out.println("工具结果: " + toolResultContent.getContent());
                    }

                    @Override
                    public void onOtherContent(Session session, AssistantContent<?> other) {
                        System.out.println("其他内容: " + other.toString());
                    }

                    @Override
                    public void onUsage(Session session, AssistantUsage assistantUsage) {
                        System.out.println("使用情况 - 输入tokens: " + 
                                assistantUsage.getUsage().getInputTokens() + 
                                ", 输出tokens: " + 
                                assistantUsage.getUsage().getOutputTokens());
                    }
                }.setDefaultPermissionOperation(Operation.allow));
        System.out.println("流式查询完成。");
    }
    
    /**
     * 代码分析功能
     */
    public List<String> analyzeCode(String code) {
        String prompt = "请分析以下代码，提供详细的解释和改进建议：\n\n" + code;
        return simpleQuery(prompt);
    }
    
    /**
     * 代码生成功能
     */
    public List<String> generateCode(String description) {
        String prompt = "请根据以下描述生成高质量的代码：\n\n" + description;
        return simpleQuery(prompt);
    }
    
    /**
     * 代码优化功能
     */
    public List<String> optimizeCode(String code, String requirements) {
        String prompt = "请根据以下要求优化代码：\n\n要求：" + requirements + "\n\n代码：\n" + code;
        return simpleQuery(prompt);
    }
    
    /**
     * 代码修复功能
     */
    public List<String> fixCode(String code, String errorDescription) {
        String prompt = "请根据错误描述修复代码中的问题：\n\n错误描述：" + errorDescription + "\n\n代码：\n" + code;
        return simpleQuery(prompt);
    }
    
    /**
     * 代码解释功能
     */
    public List<String> explainCode(String code) {
        String prompt = "请详细解释以下代码的功能和实现逻辑：\n\n" + code;
        return simpleQuery(prompt);
    }
    
    /**
     * 创建新会话
     */
    public Session newSession() {
        TransportOptions options = new TransportOptions()
                .setModel("qwen3-coder")
                .setPermissionMode(PermissionMode.PLAN);
        
        return QwenCodeCli.newSession(options);
    }
}