package com.data.assistant.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mock AI 服务 - 用于测试和演示
 * 当没有配置真实 AI 服务时使用
 */
@Service
public class MockAIService implements AIService {

    private static final Logger logger = LoggerFactory.getLogger(MockAIService.class);

    @Override
    public NL2SQLResult naturalLanguageToSQL(String naturalLanguage, String schema, Map<String, Object> context) {
        logger.info("Mock NL2SQL: {}", naturalLanguage);

        // 简单的规则匹配生成 SQL
        String sql = generateMockSQL(naturalLanguage, schema);

        NL2SQLResult result = NL2SQLResult.success(sql, NL2SQLResult.QueryIntent.QUERY);
        result.setDescription("Mock: " + naturalLanguage);
        result.setSuggestedChart(NL2SQLResult.SuggestedChart.TABLE);

        return result;
    }

    @Override
    public AnalysisResult analyzeData(String data, String question) {
        return AnalysisResult.success("Mock 分析结果：数据包含 " + data.length() + " 个字符。问题：" + question);
    }

    @Override
    public CodeResult generateCode(String description, String language) {
        String code = "// Mock " + language + " code\n" +
                      "// Description: " + description + "\n" +
                      "console.log('Hello World');";
        return CodeResult.success(code, language);
    }

    @Override
    public String explainCode(String code) {
        return "Mock 代码解释：这是一段代码，长度 " + code.length() + " 字符";
    }

    @Override
    public String chat(String prompt, Map<String, Object> context) {
        return "Mock 回复：收到消息 \"" + prompt + "\"";
    }

    @Override
    public String getProviderName() {
        return "mock";
    }

    @Override
    public boolean isAvailable() {
        return true; // Mock 服务总是可用
    }

    /**
     * 根据自然语言生成 Mock SQL
     */
    private String generateMockSQL(String naturalLanguage, String schema) {
        String lower = naturalLanguage.toLowerCase();

        // 查询所有表
        if (lower.contains("所有表") || lower.contains("有哪些表")) {
            return "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'";
        }

        // 统计数量
        if (lower.contains("统计") || lower.contains("多少") || lower.contains("count")) {
            // 尝试提取表名
            String tableName = extractTableName(lower, schema);
            if (tableName != null) {
                return "SELECT COUNT(*) FROM " + tableName;
            }
            return "SELECT COUNT(*) FROM (SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES)";
        }

        // 查询数据
        if (lower.contains("查询") || lower.contains("select") || lower.contains("查看")) {
            String tableName = extractTableName(lower, schema);
            if (tableName != null) {
                return "SELECT * FROM " + tableName + " LIMIT 100";
            }
        }

        // 默认返回
        return "SELECT 'Mock SQL for: " + naturalLanguage.replace("'", "") + "' AS result";
    }

    /**
     * 从查询中提取表名
     */
    private String extractTableName(String query, String schema) {
        // 简单的表名提取逻辑
        String[] commonTables = {"users", "orders", "products", "sales", "customers", "employees"};

        for (String table : commonTables) {
            if (query.contains(table)) {
                return table.toUpperCase();
            }
        }

        // 从 schema 中提取第一个表名
        if (schema != null && !schema.isEmpty()) {
            Pattern pattern = Pattern.compile("表名:\\s*(\\w+)");
            Matcher matcher = pattern.matcher(schema);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }
}
