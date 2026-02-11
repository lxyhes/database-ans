package com.data.assistant.service;

import cn.iflow.sdk.query.IFlowQuery;
import cn.iflow.sdk.types.messages.AssistantMessage;
import cn.iflow.sdk.types.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * iFlow Client SDK 服务集成
 * 基于 iFlow Java SDK 文档实现
 */
@Service
public class IflowClientService {
    private static final Logger logger = LoggerFactory.getLogger(IflowClientService.class);

    @Value("${iflow.enabled:true}")
    private boolean iflowEnabled;

    /**
     * 同步查询 - 使用默认配置（自动启动 iFlow 进程）
     */
    public String simpleQuery(String prompt) {
        if (!iflowEnabled) {
            logger.warn("iFlow is disabled. Returning mock response.");
            return handleFallbackResponse(prompt);
        }

        try {
            // 使用默认配置，SDK 会自动管理 iFlow 进程
            List<Message> response = IFlowQuery.querySync(prompt);

            // 提取 AssistantMessage 的文本内容
            return response.stream()
                    .filter(msg -> msg instanceof AssistantMessage)
                    .map(msg -> ((AssistantMessage) msg).getChunk().getText())
                    .collect(Collectors.joining());

        } catch (Exception e) {
            logger.error("Failed to execute simpleQuery via iFlow SDK", e);
            return handleFallbackResponse(prompt);
        }
    }

    /**
     * 异步查询 - 返回 CompletableFuture
     */
    public CompletableFuture<String> asyncQuery(String prompt) {
        if (!iflowEnabled) {
            logger.warn("iFlow is disabled. Returning mock response asynchronously.");
            return CompletableFuture.completedFuture(handleFallbackResponse(prompt));
        }

        CompletableFuture<String> future = new CompletableFuture<>();

        // 在新线程中执行查询
        new Thread(() -> {
            try {
                List<Message> response = IFlowQuery.querySync(prompt);
                String result = response.stream()
                        .filter(msg -> msg instanceof AssistantMessage)
                        .map(msg -> ((AssistantMessage) msg).getChunk().getText())
                        .collect(Collectors.joining());
                future.complete(result);
            } catch (Exception e) {
                logger.error("Async query failed", e);
                future.complete(handleFallbackResponse(prompt));
            }
        }).start();

        return future;
    }

    /**
     * 数据分析查询 - 专门用于数据分析场景
     */
    public String analyzeData(String dataContext, String question) {
        String prompt = buildDataAnalysisPrompt(dataContext, question);
        return simpleQuery(prompt);
    }

    /**
     * SQL 生成查询 - 生成 SQL 查询语句
     */
    public String generateSql(String schema, String naturalLanguageQuery) {
        String prompt = buildSqlGenerationPrompt(schema, naturalLanguageQuery);
        return simpleQuery(prompt);
    }

    /**
     * 代码生成 - 生成数据分析代码
     */
    public String generateCode(String description, String language) {
        String prompt = buildCodeGenerationPrompt(description, language);
        return simpleQuery(prompt);
    }

    /**
     * 代码解释 - 解释数据分析代码
     */
    public String explainCode(String code) {
        String prompt = buildCodeExplanationPrompt(code);
        return simpleQuery(prompt);
    }

    /**
     * 数据洞察 - 提供数据洞察和建议
     */
    public String getDataInsights(String dataSummary) {
        String prompt = buildDataInsightsPrompt(dataSummary);
        return simpleQuery(prompt);
    }

    /**
     * 构建数据分析提示词
     */
    private String buildDataAnalysisPrompt(String dataContext, String question) {
        return String.format(
                "你是一个数据分析专家。请根据以下数据上下文回答问题：\n\n" +
                        "数据上下文：\n%s\n\n" +
                        "问题：%s\n\n" +
                        "请提供详细的分析结果和可视化建议。",
                dataContext, question
        );
    }

    /**
     * 构建 SQL 生成提示词
     */
    private String buildSqlGenerationPrompt(String schema, String naturalLanguageQuery) {
        return String.format(
                "你是一个 SQL 专家。请根据以下数据库表结构，将自然语言查询转换为 SQL 语句：\n\n" +
                        "数据库表结构：\n%s\n\n" +
                        "自然语言查询：%s\n\n" +
                        "请只返回 SQL 语句，不要有任何解释。",
                schema, naturalLanguageQuery
        );
    }

    /**
     * 构建代码生成提示词
     */
    private String buildCodeGenerationPrompt(String description, String language) {
        return String.format(
                "你是一个 %s 编程专家。请根据以下描述生成高质量的代码：\n\n" +
                        "描述：%s\n\n" +
                        "请提供完整的、可运行的代码，并添加必要的注释。",
                language, description
        );
    }

    /**
     * 构建代码解释提示词
     */
    private String buildCodeExplanationPrompt(String code) {
        return String.format(
                "请详细解释以下代码的功能、逻辑和实现细节：\n\n%s\n\n" +
                        "请按以下格式提供解释：\n" +
                        "1. 代码功能概述\n" +
                        "2. 主要逻辑分析\n" +
                        "3. 关键代码说明\n" +
                        "4. 潜在改进建议",
                code
        );
    }

    /**
     * 构建数据洞察提示词
     */
    private String buildDataInsightsPrompt(String dataSummary) {
        return String.format(
                "你是一个数据洞察专家。请根据以下数据摘要提供深入的分析洞察和建议：\n\n" +
                        "数据摘要：\n%s\n\n" +
                        "请提供：\n" +
                        "1. 关键数据趋势\n" +
                        "2. 异常点识别\n" +
                        "3. 数据驱动建议\n" +
                        "4. 可视化建议",
                dataSummary
        );
    }

    /**
     * 检查 iFlow CLI 是否可用
     */
    public boolean isIflowAvailable() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("iflow", "--version");
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            logger.warn("iFlow CLI is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Fallback 响应
     */
    private String handleFallbackResponse(String prompt) {
        logger.info("Using fallback response for prompt: {}", prompt);
        return "模拟响应：当前环境未配置 iFlow SDK。您的查询 '" + prompt + "' 已接收，但无法通过 iFlow 处理。" +
                "如需启用此功能，请确保已安装 iFlow CLI 并在系统路径中可用。";
    }
}
