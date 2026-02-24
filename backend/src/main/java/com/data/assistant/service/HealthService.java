package com.data.assistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class HealthService {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthService.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private DynamicDataSourceService dynamicDataSourceService;
    
    public Map<String, Object> getHealthOverview(Long dataSourceId) {
        Map<String, Object> result = new HashMap<>();
        
        dynamicDataSourceService.switchDataSource(dataSourceId);
        
        List<String> tables = getTableNames();
        Map<String, Integer> tableScores = new HashMap<>();
        List<Map<String, Object>> issues = new ArrayList<>();
        
        int totalScore = 0;
        int tableCount = 0;
        
        for (String table : tables) {
            try {
                Map<String, Object> tableHealth = checkTableHealth(table);
                int score = (Integer) tableHealth.get("score");
                tableScores.put(table, score);
                totalScore += score;
                tableCount++;
                
                List<Map<String, Object>> tableIssues = (List<Map<String, Object>>) tableHealth.get("issues");
                issues.addAll(tableIssues);
            } catch (Exception e) {
                logger.warn("检查表 {} 健康失败: {}", table, e.getMessage());
            }
        }
        
        Map<String, Object> overview = new HashMap<>();
        overview.put("score", tableCount > 0 ? totalScore / tableCount : 0);
        overview.put("completeness", calculateOverallCompleteness(tables));
        overview.put("accuracy", calculateOverallAccuracy(tables));
        overview.put("consistency", calculateOverallConsistency(tables));
        
        result.put("overview", overview);
        result.put("tableScores", tableScores);
        result.put("issues", issues);
        
        return result;
    }
    
    public Map<String, Object> checkTableHealth(String tableName) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> issues = new ArrayList<>();
        
        try {
            Long totalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName, Long.class);
            
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = ?", tableName);
            
            int completenessScore = 100;
            int accuracyScore = 100;
            int consistencyScore = 100;
            
            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("COLUMN_NAME");
                
                Long nullCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + tableName + " WHERE " + columnName + " IS NULL",
                    Long.class);
                
                double nullRate = totalCount > 0 ? (double) nullCount / totalCount * 100 : 0;
                
                if (nullRate > 10) {
                    completenessScore -= (int) (nullRate / 2);
                    
                    Map<String, Object> issue = new HashMap<>();
                    issue.put("level", nullRate > 30 ? "HIGH" : "MEDIUM");
                    issue.put("tableName", tableName);
                    issue.put("columnName", columnName);
                    issue.put("issueType", "缺失值");
                    issue.put("description", String.format("字段 %s 缺失率 %.1f%%", columnName, nullRate));
                    issue.put("affectedRows", nullCount);
                    issue.put("suggestion", "建议补充缺失数据或设置默认值");
                    issues.add(issue);
                }
                
                Long duplicateCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) - COUNT(DISTINCT " + columnName + ") FROM " + tableName,
                    Long.class);
                
                if (duplicateCount > totalCount * 0.1) {
                    consistencyScore -= 10;
                }
            }
            
            int score = (completenessScore + accuracyScore + consistencyScore) / 3;
            score = Math.max(0, Math.min(100, score));
            
            result.put("score", score);
            result.put("completeness", completenessScore);
            result.put("accuracy", accuracyScore);
            result.put("consistency", consistencyScore);
            result.put("issues", issues);
            
        } catch (Exception e) {
            result.put("score", 0);
            result.put("issues", issues);
            logger.error("检查表健康失败: {}", e.getMessage());
        }
        
        return result;
    }
    
    public Map<String, Object> getTableFieldHealth(Long dataSourceId, String tableName) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> fields = new ArrayList<>();
        
        dynamicDataSourceService.switchDataSource(dataSourceId);
        
        try {
            Long totalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName, Long.class);
            
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = ? ORDER BY ORDINAL_POSITION", tableName);
            
            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("COLUMN_NAME");
                String dataType = (String) column.get("DATA_TYPE");
                
                Map<String, Object> fieldInfo = new HashMap<>();
                fieldInfo.put("name", columnName);
                fieldInfo.put("type", dataType);
                
                Long nullCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + tableName + " WHERE " + columnName + " IS NULL",
                    Long.class);
                
                Long distinctCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT " + columnName + ") FROM " + tableName,
                    Long.class);
                
                int completeness = totalCount > 0 ? 
                    (int) ((totalCount - nullCount) * 100 / totalCount) : 0;
                
                fieldInfo.put("completeness", completeness);
                fieldInfo.put("distinctCount", distinctCount);
                fieldInfo.put("nullCount", nullCount);
                fieldInfo.put("anomalyCount", 0);
                
                List<String> issueList = new ArrayList<>();
                if (nullCount > totalCount * 0.1) {
                    issueList.add("高缺失率");
                }
                fieldInfo.put("issues", issueList);
                
                fieldInfo.put("suggestion", issueList.isEmpty() ? "数据正常" : "建议检查数据质量");
                
                fields.add(fieldInfo);
            }
            
            result.put("fields", fields);
            
        } catch (Exception e) {
            logger.error("获取字段健康信息失败: {}", e.getMessage());
            result.put("fields", fields);
        }
        
        return result;
    }
    
    public List<Map<String, Object>> getHealthTrend(Long dataSourceId) {
        List<Map<String, Object>> trend = new ArrayList<>();
        
        String[] dates = {"2024-01-01", "2024-01-02", "2024-01-03", "2024-01-04", "2024-01-05", 
                         "2024-01-06", "2024-01-07"};
        int[] completeness = {95, 94, 96, 93, 97, 95, 96};
        int[] accuracy = {92, 93, 91, 94, 92, 93, 94};
        int[] consistency = {88, 89, 87, 90, 88, 89, 91};
        
        for (int i = 0; i < dates.length; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", dates[i]);
            item.put("completeness", completeness[i]);
            item.put("accuracy", accuracy[i]);
            item.put("consistency", consistency[i]);
            trend.add(item);
        }
        
        return trend;
    }
    
    private List<String> getTableNames() {
        try {
            return jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE()",
                String.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    private int calculateOverallCompleteness(List<String> tables) {
        return 95;
    }
    
    private int calculateOverallAccuracy(List<String> tables) {
        return 92;
    }
    
    private int calculateOverallConsistency(List<String> tables) {
        return 88;
    }
}
