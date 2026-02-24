package com.data.assistant.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.*;

@Service
public class SqlOptimizationService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private DynamicDataSourceService dynamicDataSourceService;

    public Map<String, Object> analyzeSql(String sql) {
        return analyzeSql(sql, null);
    }
    
    public Map<String, Object> analyzeSql(String sql, Long dataSourceId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> suggestions = new ArrayList<>();
        int score = 100;

        if (dataSourceId != null) {
            try {
                dynamicDataSourceService.switchDataSource(dataSourceId);
            } catch (Exception e) {
                result.put("dataSourceError", e.getMessage());
            }
        }

        if (sql.matches("(?i).*SELECT\\s+\\*.*")) {
            suggestions.add(createSuggestion(
                "避免使用SELECT *",
                "查询中使用了SELECT *，建议只查询需要的字段，减少网络传输和内存占用",
                "high",
                "将SELECT * 改为具体的字段列表"
            ));
            score -= 15;
        }

        if (sql.matches("(?i).*(UPDATE|DELETE).*") && !sql.matches("(?i).*WHERE.*")) {
            suggestions.add(createSuggestion(
                "缺少WHERE条件",
                "UPDATE或DELETE语句缺少WHERE条件，可能导致全表数据被修改或删除",
                "critical",
                "添加适当的WHERE条件限制影响范围"
            ));
            score -= 30;
        }

        if (sql.matches("(?i).*WHERE.*\\d+.*")) {
            suggestions.add(createSuggestion(
                "可能的隐式类型转换",
                "WHERE条件中数字与字符串字段比较可能导致索引失效",
                "medium",
                "确保比较操作符两侧类型一致"
            ));
            score -= 10;
        }

        if (sql.matches("(?i).*LIKE\\s+'%.*")) {
            suggestions.add(createSuggestion(
                "前缀模糊查询",
                "使用了'%xxx'形式的前缀模糊查询，无法使用索引",
                "medium",
                "考虑使用全文索引或反向索引优化"
            ));
            score -= 10;
        }

        if (sql.matches("(?i).*WHERE.*\\bOR\\b.*")) {
            suggestions.add(createSuggestion(
                "使用OR条件",
                "OR条件可能导致索引失效，考虑使用UNION或IN替代",
                "low",
                "将OR改为UNION或IN"
            ));
            score -= 5;
        }

        if (sql.matches("(?i).*SELECT.*\\(SELECT.*")) {
            suggestions.add(createSuggestion(
                "使用子查询",
                "子查询性能较差，考虑改为JOIN",
                "medium",
                "将子查询改为JOIN语法"
            ));
            score -= 10;
        }

        if (sql.matches("(?i).*NOT\\s+IN.*")) {
            suggestions.add(createSuggestion(
                "使用NOT IN",
                "NOT IN在子查询返回NULL时结果不正确，且性能较差",
                "medium",
                "使用NOT EXISTS替代NOT IN"
            ));
            score -= 10;
        }

        if (sql.matches("(?i).*ORDER\\s+BY\\s+RAND\\s*\\(.*")) {
            suggestions.add(createSuggestion(
                "使用ORDER BY RAND()",
                "ORDER BY RAND()会导致全表扫描和文件排序，性能极差",
                "high",
                "使用应用层随机或优化算法替代"
            ));
            score -= 20;
        }

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
        
        try {
            Map<String, Object> explainResult = executeExplain(sql);
            result.put("explain", explainResult);
            
            List<Map<String, Object>> explainSuggestions = analyzeExplain(explainResult);
            suggestions.addAll(explainSuggestions);
            
            for (Map<String, Object> s : explainSuggestions) {
                String severity = (String) s.get("severity");
                if ("critical".equals(severity)) score -= 20;
                else if ("high".equals(severity)) score -= 10;
                else if ("medium".equals(severity)) score -= 5;
            }
        } catch (Exception e) {
            result.put("explainError", "无法获取执行计划: " + e.getMessage());
        }

        result.put("score", Math.max(0, score));
        result.put("grade", score >= 90 ? "A" : score >= 70 ? "B" : score >= 50 ? "C" : "D");
        result.put("suggestions", suggestions);
        result.put("totalSuggestions", suggestions.size());

        return result;
    }
    
    private Map<String, Object> executeExplain(String sql) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String explainSql = "EXPLAIN " + sql;
            List<Map<String, Object>> explainRows = jdbcTemplate.queryForList(explainSql);
            result.put("rows", explainRows);
            
            String explainFormatSql = "EXPLAIN FORMAT=JSON " + sql;
            try {
                String jsonResult = jdbcTemplate.queryForObject(explainFormatSql, String.class);
                result.put("json", jsonResult);
            } catch (Exception ignored) {
            }
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    private List<Map<String, Object>> analyzeExplain(Map<String, Object> explainResult) {
        List<Map<String, Object>> suggestions = new ArrayList<>();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) explainResult.get("rows");
        if (rows == null) return suggestions;
        
        for (Map<String, Object> row : rows) {
            String type = (String) row.get("type");
            String key = (String) row.get("key");
            Long rowsExamined = row.get("rows") != null ? ((Number) row.get("rows")).longValue() : 0;
            String extra = (String) row.get("Extra");
            String table = (String) row.get("table");
            
            if ("ALL".equals(type)) {
                suggestions.add(createSuggestion(
                    "全表扫描",
                    "表 " + table + " 使用了全表扫描(type=ALL)，这是最慢的访问方式",
                    "critical",
                    "为查询条件添加适当的索引"
                ));
            } else if ("index".equals(type)) {
                suggestions.add(createSuggestion(
                    "索引全扫描",
                    "表 " + table + " 使用了索引全扫描(type=index)，效率较低",
                    "medium",
                    "考虑优化索引或查询条件"
                ));
            }
            
            if (key == null && !"system".equals(type) && !"const".equals(type)) {
                suggestions.add(createSuggestion(
                    "未使用索引",
                    "表 " + table + " 没有使用任何索引",
                    "high",
                    "检查WHERE条件并添加合适的索引"
                ));
            }
            
            if (rowsExamined > 10000) {
                suggestions.add(createSuggestion(
                    "扫描行数过多",
                    "表 " + table + " 预计扫描 " + rowsExamined + " 行，可能影响性能",
                    "high",
                    "优化查询条件减少扫描行数"
                ));
            }
            
            if (extra != null) {
                if (extra.contains("Using filesort")) {
                    suggestions.add(createSuggestion(
                        "使用文件排序",
                        "表 " + table + " 使用了文件排序(Using filesort)，性能较差",
                        "medium",
                        "为ORDER BY字段添加索引"
                    ));
                }
                if (extra.contains("Using temporary")) {
                    suggestions.add(createSuggestion(
                        "使用临时表",
                        "表 " + table + " 使用了临时表(Using temporary)",
                        "medium",
                        "优化GROUP BY或ORDER BY避免临时表"
                    ));
                }
                if (extra.contains("Using join buffer")) {
                    suggestions.add(createSuggestion(
                        "使用JOIN缓冲区",
                        "表 " + table + " 使用了JOIN缓冲区，可能缺少索引",
                        "low",
                        "为JOIN条件添加索引"
                    ));
                }
            }
        }
        
        return suggestions;
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
        return optimizeSql(sql, null);
    }
    
    public String optimizeSql(String sql, Long dataSourceId) {
        String optimized = sql;

        optimized = optimized.replaceAll("(?i)WHERE\\s+(.+)\\s+OR\\s+(.+)", 
            "WHERE $1 UNION SELECT * FROM table WHERE $2");

        optimized = optimized.replaceAll("(?i)NOT\\s+IN\\s*\\(([^)]+)\\)", 
            "NOT EXISTS (SELECT 1 FROM table WHERE condition)");

        if (!optimized.matches("(?i).*LIMIT.*") && optimized.matches("(?i).*SELECT.*")) {
            optimized = optimized + " LIMIT 1000";
        }

        return optimized;
    }
    
    public Map<String, Object> getIndexSuggestions(String tableName) {
        return getIndexSuggestions(tableName, null);
    }
    
    public Map<String, Object> getIndexSuggestions(String tableName, Long dataSourceId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> suggestions = new ArrayList<>();
        
        if (dataSourceId != null) {
            try {
                dynamicDataSourceService.switchDataSource(dataSourceId);
            } catch (Exception e) {
                result.put("error", e.getMessage());
                return result;
            }
        }
        
        try {
            List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
                "SHOW INDEX FROM " + tableName
            );
            result.put("existingIndexes", indexes);
            
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ?",
                tableName
            );
            result.put("columns", columns);
            
            for (Map<String, Object> col : columns) {
                String colName = (String) col.get("COLUMN_NAME");
                String dataType = (String) col.get("DATA_TYPE");
                
                if (colName.toLowerCase().endsWith("_id") || 
                    colName.toLowerCase().endsWith("_code") ||
                    colName.toLowerCase().equals("id")) {
                    
                    boolean hasIndex = indexes.stream()
                        .anyMatch(idx -> colName.equals(idx.get("COLUMN_NAME")));
                    
                    if (!hasIndex) {
                        suggestions.add(Map.of(
                            "type", "INDEX",
                            "column", colName,
                            "reason", "外键或ID字段通常需要索引",
                            "sql", "CREATE INDEX idx_" + colName + " ON " + tableName + "(" + colName + ")"
                        ));
                    }
                }
                
                if (colName.toLowerCase().contains("time") || 
                    colName.toLowerCase().contains("date") ||
                    colName.toLowerCase().endsWith("_at")) {
                    
                    boolean hasIndex = indexes.stream()
                        .anyMatch(idx -> colName.equals(idx.get("COLUMN_NAME")));
                    
                    if (!hasIndex) {
                        suggestions.add(Map.of(
                            "type", "INDEX",
                            "column", colName,
                            "reason", "时间字段常用于范围查询和排序",
                            "sql", "CREATE INDEX idx_" + colName + " ON " + tableName + "(" + colName + ")"
                        ));
                    }
                }
            }
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        result.put("suggestions", suggestions);
        return result;
    }
    
    public Map<String, Object> getTableStatistics(String tableName) {
        return getTableStatistics(tableName, null);
    }
    
    public Map<String, Object> getTableStatistics(String tableName, Long dataSourceId) {
        Map<String, Object> result = new HashMap<>();
        
        if (dataSourceId != null) {
            try {
                dynamicDataSourceService.switchDataSource(dataSourceId);
            } catch (Exception e) {
                result.put("error", e.getMessage());
                return result;
            }
        }
        
        try {
            Map<String, Object> tableStatus = jdbcTemplate.queryForMap(
                "SHOW TABLE STATUS LIKE ?", tableName
            );
            result.put("tableStatus", tableStatus);
            
            Long rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName, Long.class
            );
            result.put("rowCount", rowCount);
            
            List<Map<String, Object>> columnStats = new ArrayList<>();
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ?",
                tableName
            );
            
            for (Map<String, Object> col : columns) {
                String colName = (String) col.get("COLUMN_NAME");
                String dataType = (String) col.get("DATA_TYPE");
                
                Map<String, Object> stats = new HashMap<>();
                stats.put("column", colName);
                stats.put("type", dataType);
                
                if (dataType.contains("int") || dataType.contains("decimal") || 
                    dataType.contains("float") || dataType.contains("double")) {
                    try {
                        Map<String, Object> numStats = jdbcTemplate.queryForMap(
                            "SELECT MIN(" + colName + ") as min, MAX(" + colName + ") as max, " +
                            "AVG(" + colName + ") as avg, COUNT(DISTINCT " + colName + ") as distinct_count " +
                            "FROM " + tableName
                        );
                        stats.put("statistics", numStats);
                    } catch (Exception ignored) {}
                }
                
                columnStats.add(stats);
            }
            
            result.put("columnStatistics", columnStats);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}
