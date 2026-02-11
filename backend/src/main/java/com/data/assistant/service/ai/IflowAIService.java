package com.data.assistant.service.ai;

import com.data.assistant.service.IflowClientService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * iFlow AI 服务实现
 */
@Service
public class IflowAIService implements AIService {

    private static final Logger logger = LoggerFactory.getLogger(IflowAIService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private IflowClientService iflowClientService;

    @Override
    public NL2SQLResult naturalLanguageToSQL(String naturalLanguage, String schema, Map<String, Object> context) {
        try {
            // 构建 NL2SQL 提示词
            String prompt = buildNL2SQLPrompt(naturalLanguage, schema, context);

            // 调用 iFlow
            String response = iflowClientService.generateSql(schema, naturalLanguage);

            // 解析结果
            return parseNL2SQLResponse(response);
        } catch (Exception e) {
            logger.error("iFlow NL2SQL failed", e);
            return NL2SQLResult.failure("NL2SQL 转换失败: " + e.getMessage());
        }
    }

    @Override
    public AnalysisResult analyzeData(String data, String question) {
        try {
            String response = iflowClientService.analyzeData(data, question);
            return AnalysisResult.success(response);
        } catch (Exception e) {
            logger.error("iFlow analysis failed", e);
            return AnalysisResult.failure("数据分析失败: " + e.getMessage());
        }
    }

    @Override
    public CodeResult generateCode(String description, String language) {
        try {
            String response = iflowClientService.generateCode(description, language);
            return CodeResult.success(response, language);
        } catch (Exception e) {
            logger.error("iFlow code generation failed", e);
            return CodeResult.failure("代码生成失败: " + e.getMessage());
        }
    }

    @Override
    public String explainCode(String code) {
        try {
            return iflowClientService.explainCode(code);
        } catch (Exception e) {
            logger.error("iFlow code explanation failed", e);
            return "代码解释失败: " + e.getMessage();
        }
    }

    @Override
    public String chat(String prompt, Map<String, Object> context) {
        try {
            return iflowClientService.simpleQuery(prompt);
        } catch (Exception e) {
            logger.error("iFlow chat failed", e);
            return "对话失败: " + e.getMessage();
        }
    }

    @Override
    public String getProviderName() {
        return "iFlow";
    }

    @Override
    public boolean isAvailable() {
        try {
            // 简单的健康检查
            iflowClientService.simpleQuery("test");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 构建 NL2SQL 提示词
     */
    private String buildNL2SQLPrompt(String naturalLanguage, String schema, Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个专业的 SQL 生成助手。请根据以下数据库 Schema 和自然语言查询生成 SQL 语句。\n\n");

        // Schema 信息
        prompt.append("数据库 Schema:\n");
        prompt.append(schema);
        prompt.append("\n\n");

        // 多轮对话上下文
        if (context != null && context.containsKey("history")) {
            prompt.append("对话历史:\n");
            prompt.append(context.get("history"));
            prompt.append("\n\n");
        }

        // 当前查询
        prompt.append("用户查询: ").append(naturalLanguage).append("\n\n");

        // 输出要求
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
            // 尝试解析 JSON
            JsonNode json = objectMapper.readTree(response);

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
            // 如果不是 JSON，直接返回 SQL
            return NL2SQLResult.success(response.trim(), NL2SQLResult.QueryIntent.QUERY);
        }
    }
}
