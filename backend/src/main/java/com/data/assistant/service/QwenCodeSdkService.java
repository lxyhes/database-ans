package com.data.assistant.service;

import com.alibaba.qwen.code.cli.QwenCodeCli;
import com.alibaba.qwen.code.cli.transport.TransportOptions;
import com.alibaba.qwen.code.cli.session.event.consumers.AssistantContentSimpleConsumers;
import com.alibaba.qwen.code.cli.protocol.data.*;
import com.alibaba.qwen.code.cli.protocol.data.behavior.Behavior.Operation;
import com.alibaba.qwen.code.cli.session.Session;
import com.alibaba.qwen.code.cli.utils.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
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
    private static final Logger logger = LoggerFactory.getLogger(QwenCodeSdkService.class);

    @Value("${qwen.code.sdk.enabled:true}")
    private boolean qwenCodeSdkEnabled;

    /**
     * 简单查询方法 - 对应文档中的 QwenCodeCli.simpleQuery()
     */
    public List<String> simpleQuery(String prompt) {
        if (!qwenCodeSdkEnabled || !isQwenCommandAvailable()) {
            logger.warn("Qwen Code SDK is disabled or qwen command is not available. Returning mock response.");
            return Arrays.asList(handleFallbackResponse(prompt));
        }

        try {
            return QwenCodeCli.simpleQuery(prompt);
        } catch (Exception e) {
            logger.error("Failed to execute simpleQuery via Qwen Code SDK, falling back to mock response", e);
            return Arrays.asList(handleFallbackResponse(prompt));
        }
    }

    /**
     * 带传输选项的查询方法 - 对应文档中的高级用法
     */
    public List<String> queryWithOptions(String prompt) {
        if (!qwenCodeSdkEnabled || !isQwenCommandAvailable()) {
            logger.warn("Qwen Code SDK is disabled or qwen command is not available. Returning mock response.");
            return Arrays.asList(handleFallbackResponse(prompt));
        }

        try {
            TransportOptions options = new TransportOptions()
                    .setModel("qwen3-coder")
                    .setPermissionMode(PermissionMode.AUTO_EDIT)
                    .setCwd("./")
                    .setIncludePartialMessages(true)
                    .setTurnTimeout(new Timeout(120L, TimeUnit.SECONDS))
                    .setMessageTimeout(new Timeout(90L, TimeUnit.SECONDS));

            return QwenCodeCli.simpleQuery(prompt, options);
        } catch (Exception e) {
            logger.error("Failed to execute queryWithOptions via Qwen Code SDK, falling back to mock response", e);
            return Arrays.asList(handleFallbackResponse(prompt));
        }
    }

    /**
     * 流式内容处理 - 对应文档中的流式示例
     */
    public void streamQuery(String prompt) {
        if (!qwenCodeSdkEnabled || !isQwenCommandAvailable()) {
            logger.warn("Qwen Code SDK is disabled or qwen command is not available. Skipping stream query.");
            System.out.println("模拟流式查询完成。");
            return;
        }

        try {
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
        } catch (Exception e) {
            logger.error("Failed to execute streamQuery via Qwen Code SDK", e);
            System.out.println("流式查询失败，使用模拟响应。");
        }
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
        if (!qwenCodeSdkEnabled || !isQwenCommandAvailable()) {
            logger.warn("Qwen Code SDK is disabled or qwen command is not available. Cannot create session.");
            return null;
        }

        try {
            TransportOptions options = new TransportOptions()
                    .setModel("qwen3-coder")
                    .setPermissionMode(PermissionMode.PLAN);

            return QwenCodeCli.newSession(options);
        } catch (Exception e) {
            logger.error("Failed to create new session via Qwen Code SDK", e);
            return null;
        }
    }

    /**
     * Checks if the qwen command is available in the system
     */
    private boolean isQwenCommandAvailable() {
        try {
            // Try to execute 'qwen --help' to check if the command exists
            ProcessBuilder processBuilder = new ProcessBuilder("qwen", "--help");
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode == 0 || exitCode == 1; // 0 for success, 1 for help displayed
        } catch (Exception e) {
            logger.warn("qwen command is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Fallback response when Qwen Code SDK is not available
     */
    private String handleFallbackResponse(String prompt) {
        logger.info("Using fallback response for prompt: {}", prompt);
        // Return a meaningful response that indicates the limitation
        return "模拟响应：当前环境未配置 Qwen Code SDK。您的查询 '" + prompt + "' 已接收，但无法通过 Qwen Code SDK 处理。" +
               "如需启用此功能，请确保已安装 Qwen CLI 并在系统路径中可用。";
    }
}