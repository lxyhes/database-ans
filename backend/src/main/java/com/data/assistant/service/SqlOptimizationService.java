package com.data.assistant.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.*;

@Service
public class SqlOptimizationService {

    public Map<String, Object> analyzeSql(String sql) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> suggestions = new ArrayList<>();
        int score = 100;

        // 1. 检查SELECT *
        if (sql.matches("(?i).*SELECT\\s+\\*.*")) {
            suggestions.add(createSuggestion(
                "避免使用SELECT *",
                "查询中使用了SELECT *，建议只查询需要的字段，减少网络传输和内存占用",
                "high",
                "将SELECT * 改为具体的字段列表"
            ));
            score -= 15;
        }

        // 2. 检查缺少WHERE子句的UPDATE/DELETE
        if (sql.matches("(?i).*(UPDATE|DELETE).*") && !sql.matches("(?i).*WHERE.*")) {
            suggestions.add(createSuggestion(
                "缺少WHERE条件",
                "UPDATE或DELETE语句缺少WHERE条件，可能导致全表数据被修改或删除",
                "critical",
                "添加适当的WHERE条件限制影响范围"
            ));
            score -= 30;
        }

        // 3. 检查隐式类型转换
        if (sql.matches("(?i).*WHERE.*\\d+.*")) {
            suggestions.add(createSuggestion(
                "可能的隐式类型转换",
                "WHERE条件中数字与字符串字段比较可能导致索引失效",
                "medium",
                "确保比较操作符两侧类型一致"
            ));
            score -= 10;
        }

        // 4. 检查LIKE前缀模糊查询
        if (sql.matches("(?i).*LIKE\\s+'%.*")) {
            suggestions.add(createSuggestion(
                "前缀模糊查询",
                "使用了'%xxx'形式的前缀模糊查询，无法使用索引",
                "medium",
                "考虑使用全文索引或反向索引优化"
            ));
            score -= 10;
        }

        // 5. 检查OR条件
        if (sql.matches("(?i).*WHERE.*\\bOR\\b.*")) {
            suggestions.add(createSuggestion(
                "使用OR条件",
                "OR条件可能导致索引失效，考虑使用UNION或IN替代",
                "low",
                "将OR改为UNION或IN"
            ));
            score -= 5;
        }

        // 6. 检查子查询
        if (sql.matches("(?i).*SELECT.*\\(SELECT.*")) {
            suggestions.add(createSuggestion(
                "使用子查询",
                "子查询性能较差，考虑改为JOIN",
                "medium",
                "将子查询改为JOIN语法"
            ));
            score -= 10;
        }

        // 7. 检查NOT IN
        if (sql.matches("(?i).*NOT\\s+IN.*")) {
            suggestions.add(createSuggestion(
                "使用NOT IN",
                "NOT IN在子查询返回NULL时结果不正确，且性能较差",
                "medium",
                "使用NOT EXISTS替代NOT IN"
            ));
            score -= 10;
        }

        // 8. 检查ORDER BY RAND()
        if (sql.matches("(?i).*ORDER\\s+BY\\s+RAND\\s*\\(.*")) {
            suggestions.add(createSuggestion(
                "使用ORDER BY RAND()",
                "ORDER BY RAND()会导致全表扫描和文件排序，性能极差",
                "high",
                "使用应用层随机或优化算法替代"
            ));
            score -= 20;
        }

        // 9. 检查LIMIT大偏移量
        Pattern limitPattern = Pattern.compile("(?i)LIMIT\\s+\\d+\\s*,\\s*(\\d+)");
        Matcher limitMatcher = limitPattern.matcher(sql);
        if (limitMatcher.find()) {
            int offset = Integer.parseInt(limitMatcher.group(1));
            if (offset > 10000) {
                suggestions.add(createSuggestion(
                    "大偏移量分页",
                    "LIMIT大偏移量会导致扫描大量无用数据",
                    "high",
                    "使用覆盖索引或延迟关联优化分页"
                ));
                score -= 15;
            }
        }

        // 10. 检查多表JOIN
        Pattern joinPattern = Pattern.compile("(?i)\\bJOIN\\b");
        Matcher joinMatcher = joinPattern.matcher(sql);
        int joinCount = 0;
        while (joinMatcher.find()) joinCount++;
        if (joinCount > 3) {
            suggestions.add(createSuggestion(
                "多表JOIN",
                "查询涉及" + joinCount + "个表JOIN，可能导致性能问题",
                "medium",
                "考虑拆分查询或优化JOIN顺序"
            ));
            score -= 10;
        }

        result.put("score", Math.max(0, score));
        result.put("grade", score >= 90 ? "A" : score >= 70 ? "B" : score >= 50 ? "C" : "D");
        result.put("suggestions", suggestions);
        result.put("totalSuggestions", suggestions.size());

        return result;
    }

    private Map<String, Object> createSuggestion(String title, String description, String severity, String solution) {
        Map<String, Object> suggestion = new HashMap<>();
        suggestion.put("title", title);
        suggestion.put("description", description);
        suggestion.put("severity", severity);
        suggestion.put("solution", solution);
        return suggestion;
    }

    public String optimizeSql(String sql) {
        String optimized = sql;

        // 1. 将OR改为UNION（简单情况）
        optimized = optimized.replaceAll("(?i)WHERE\\s+(.+)\\s+OR\\s+(.+)", 
            "WHERE $1 UNION SELECT * FROM table WHERE $2");

        // 2. 将NOT IN改为NOT EXISTS
        optimized = optimized.replaceAll("(?i)NOT\\s+IN\\s*\\(([^)]+)\\)", 
            "NOT EXISTS (SELECT 1 FROM table WHERE condition)");

        // 3. 添加LIMIT限制（如果没有）
        if (!optimized.matches("(?i).*LIMIT.*") && optimized.matches("(?i).*SELECT.*")) {
            optimized = optimized + " LIMIT 1000";
        }

        return optimized;
    }
}
