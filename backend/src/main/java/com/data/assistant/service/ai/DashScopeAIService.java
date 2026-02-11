package com.data.assistant.service.ai;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Map;

/**
 * DashScope (阿里云灵积模型服务) AI 服务实现
 * 使用通义千问大模型提供真实的 AI 能力
 */
@Service
public class DashScopeAIService implements AIService {

    private static final Logger logger = LoggerFactory.getLogger(DashScopeAIService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${dashscope.api.key:}")
    private String apiKey;

    @Value("${dashscope.model:qwen-turbo}")
    private String model;

    private Generation generation;
    private boolean available = false;

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                generation = new Generation();
                available = true;
                logger.info("DashScope AI service initialized with model: {}", model);
            } catch (Exception e) {
                logger.error("Failed to initialize DashScope service", e);
                available = false;
            }
        } else {
            logger.warn("DashScope API key not configured, service will be unavailable");
            available = false;
        }
    }

    @Override
    public NL2SQLResult naturalLanguageToSQL(String naturalLanguage, String schema, Map<String, Object> context) {
        if (!available) {
            return NL2SQLResult.failure("DashScope 服务未配置，请设置 API Key");
        }

        try {
            String prompt = buildNL2SQLPrompt(naturalLanguage, schema, context);
            String response = callDashScope(prompt);
            return parseNL2SQLResponse(response);
        } catch (Exception e) {
            logger.error("DashScope NL2SQL failed", e);
            return NL2SQLResult.failure("NL2SQL 转换失败: " + e.getMessage());
        }
    }

    @Override
    public AnalysisResult analyzeData(String data, String question) {
        if (!available) {
            return AnalysisResult.failure("DashScope 服务未配置，请设置 API Key");
        }

        try {
            String prompt = buildDataAnalysisPrompt(data, question);
            String response = callDashScope(prompt);
            return AnalysisResult.success(response);
        } catch (Exception e) {
            logger.error("DashScope analysis failed", e);
            return AnalysisResult.failure("数据分析失败: " + e.getMessage());
        }
    }

    @Override
    public CodeResult generateCode(String description, String language) {
        if (!available) {
            return CodeResult.failure("DashScope 服务未配置，请设置 API Key");
        }

        try {
            String prompt = buildCodeGenerationPrompt(description, language);
            String response = callDashScope(prompt);
            return CodeResult.success(response, language);
        } catch (Exception e) {
            logger.error("DashScope code generation failed", e);
            return CodeResult.failure("代码生成失败: " + e.getMessage());
        }
    }

    @Override
    public String explainCode(String code) {
        if (!available) {
            return "DashScope 服务未配置，请设置 API Key";
        }

        try {
            String prompt = buildCodeExplanationPrompt(code);
            return callDashScope(prompt);
        } catch (Exception e) {
            logger.error("DashScope code explanation failed", e);
            return "代码解释失败: " + e.getMessage();
        }
    }

    @Override
    public String chat(String prompt, Map<String, Object> context) {
        if (!available) {
            return "DashScope 服务未配置，请设置 API Key";
        }

        try {
            return callDashScope(prompt);
        } catch (Exception e) {
            logger.error("DashScope chat failed", e);
            return "对话失败: " + e.getMessage();
        }
    }

    @Override
    public String getProviderName() {
        return "dashscope";
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    /**
     * 调用 DashScope API
     */
    private String callDashScope(String prompt) throws NoApiKeyException, InputRequiredException {
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(prompt)
                .build();

        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .messages(Arrays.asList(userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .maxTokens(2000)
                .temperature(0.7f)
                .build();

        GenerationResult result = generation.call(param);
        return result.getOutput().getChoices().get(0).getMessage().getContent();
    }

    /**
     * 构建 NL2SQL 提示词
     */
    private String buildNL2SQLPrompt(String naturalLanguage, String schema, Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个专业的 SQL 生成助手。请根据以下数据库 Schema 和自然语言查询生成 SQL 语句。\n\n");

        if (schema != null && !schema.isEmpty()) {
            prompt.append("数据库 Schema:\n");
            prompt.append(schema);
            prompt.append("\n\n");
        }

        if (context != null && context.containsKey("history")) {
            prompt.append("对话历史:\n");
            prompt.append(context.get("history"));
            prompt.append("\n\n");
        }

        prompt.append("用户查询: ").append(naturalLanguage).append("\n\n");

        prompt.append("请生成 SQL 语句，并以 JSON 格式返回:\n");
        prompt.append("{\n");
        prompt.append("  \"sql\": \"生成的 SQL 语句\",\n");
        prompt.append("  \"intent\": \"QUERY|ANALYSIS|REPORT\",\n");
        prompt.append("  \"description\": \"查询描述\",\n");
        prompt.append("  \"suggestedChart\": \"TABLE|LINE|BAR|PIE|NONE\"\n");
        prompt.append("}");

        return prompt.toString();
    }

    /**
     * 解析 NL2SQL 响应
     */
    private NL2SQLResult parseNL2SQLResponse(String response) {
        try {
            // 提取 JSON 部分
            String jsonStr = extractJson(response);
            JsonNode json = objectMapper.readTree(jsonStr);

            String sql = json.has("sql") ? json.get("sql").asText() : null;
            String intentStr = json.has("intent") ? json.get("intent").asText() : "QUERY";
            String description = json.has("description") ? json.get("description").asText() : null;
            String chartStr = json.has("suggestedChart") ? json.get("suggestedChart").asText() : "NONE";

            NL2SQLResult.QueryIntent intent = NL2SQLResult.QueryIntent.UNKNOWN;
            try {
                intent = NL2SQLResult.QueryIntent.valueOf(intentStr);
            } catch (Exception ignored) {}

            NL2SQLResult.SuggestedChart chart = NL2SQLResult.SuggestedChart.NONE;
            try {
                chart = NL2SQLResult.SuggestedChart.valueOf(chartStr);
            } catch (Exception ignored) {}

            NL2SQLResult result = NL2SQLResult.success(sql, intent);
            result.setDescription(description);
            result.setSuggestedChart(chart);

            return result;
        } catch (Exception e) {
            // 如果不是 JSON，尝试直接提取 SQL
            String sql = extractSQL(response);
            if (sql != null) {
                return NL2SQLResult.success(sql, NL2SQLResult.QueryIntent.QUERY);
            }
            return NL2SQLResult.success(response.trim(), NL2SQLResult.QueryIntent.QUERY);
        }
    }

    /**
     * 从响应中提取 JSON
     */
    private String extractJson(String response) {
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    /**
     * 从响应中提取 SQL
     */
    private String extractSQL(String response) {
        // 尝试提取 ```sql ... ``` 格式的代码块
        if (response.contains("```sql")) {
            int start = response.indexOf("```sql") + 6;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }
        // 尝试提取 ``` ... ``` 格式的代码块
        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }
        return null;
    }

    /**
     * 构建数据分析提示词
     */
    private String buildDataAnalysisPrompt(String data, String question) {
        return String.format(
                "你是一个数据分析专家。请根据以下数据回答问题：\n\n" +
                        "数据：\n%s\n\n" +
                        "问题：%s\n\n" +
                        "请提供详细的分析结果，包括关键发现、趋势分析和建议。",
                data, question
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
}
