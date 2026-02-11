package com.data.assistant.service;

import com.data.assistant.repository.DataSourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
public class DataStoryService {

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private DataSource jdbcDataSource;

    public Map<String, Object> generateDataStory(Long dataSourceId, String tableName) {
        Map<String, Object> story = new HashMap<>();
        
        try {
            // 1. 获取表的基本统计信息
            Map<String, Object> statistics = getTableStatistics(dataSourceId, tableName);
            
            // 2. 获取数值列的分布
            List<Map<String, Object>> columnDistributions = getColumnDistributions(dataSourceId, tableName);
            
            // 3. 生成洞察
            List<String> insights = generateInsights(statistics, columnDistributions);
            
            // 4. 生成故事文本
            String storyText = generateStoryText(tableName, statistics, insights);
            
            story.put("title", tableName + " 数据洞察报告");
            story.put("tableName", tableName);
            story.put("statistics", statistics);
            story.put("columnDistributions", columnDistributions);
            story.put("insights", insights);
            story.put("storyText", storyText);
            story.put("generatedAt", new java.util.Date());
            
        } catch (Exception e) {
            story.put("error", e.getMessage());
        }
        
        return story;
    }

    private Map<String, Object> getTableStatistics(Long dataSourceId, String tableName) throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        
        try (Connection connection = jdbcDataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            
            // 总行数
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName);
            if (rs.next()) {
                stats.put("totalRows", rs.getLong(1));
            }
            
            // 获取列信息
            DatabaseMetaData metaData = connection.getMetaData();
            List<Map<String, Object>> columns = new ArrayList<>();
            
            try (ResultSet colRs = metaData.getColumns(null, null, tableName, "%")) {
                while (colRs.next()) {
                    Map<String, Object> col = new HashMap<>();
                    col.put("name", colRs.getString("COLUMN_NAME"));
                    col.put("type", colRs.getString("TYPE_NAME"));
                    columns.add(col);
                }
            }
            stats.put("columns", columns);
            stats.put("columnCount", columns.size());
            
            // 数值列统计
            for (Map<String, Object> col : columns) {
                String colName = (String) col.get("name");
                String colType = ((String) col.get("type")).toUpperCase();
                
                if (colType.contains("INT") || colType.contains("DECIMAL") || 
                    colType.contains("FLOAT") || colType.contains("DOUBLE") ||
                    colType.contains("NUMERIC")) {
                    
                    String sql = String.format(
                        "SELECT MIN(%s) as min, MAX(%s) as max, AVG(%s) as avg, " +
                        "COUNT(DISTINCT %s) as unique_count FROM %s",
                        colName, colName, colName, colName, tableName
                    );
                    
                    try (ResultSet statRs = stmt.executeQuery(sql)) {
                        if (statRs.next()) {
                            Map<String, Object> colStats = new HashMap<>();
                            colStats.put("min", statRs.getObject("min"));
                            colStats.put("max", statRs.getObject("max"));
                            colStats.put("avg", statRs.getObject("avg"));
                            colStats.put("uniqueCount", statRs.getLong("unique_count"));
                            col.put("statistics", colStats);
                        }
                    }
                }
            }
        }
        
        return stats;
    }

    private List<Map<String, Object>> getColumnDistributions(Long dataSourceId, String tableName) throws SQLException {
        List<Map<String, Object>> distributions = new ArrayList<>();
        
        try (Connection connection = jdbcDataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            
            // 获取所有列
            DatabaseMetaData metaData = connection.getMetaData();
            List<String> columns = new ArrayList<>();
            
            try (ResultSet colRs = metaData.getColumns(null, null, tableName, "%")) {
                while (colRs.next()) {
                    columns.add(colRs.getString("COLUMN_NAME"));
                }
            }
            
            // 对每个列获取TOP值分布
            for (String column : columns) {
                Map<String, Object> dist = new HashMap<>();
                dist.put("column", column);
                
                String sql = String.format(
                    "SELECT %s as value, COUNT(*) as count FROM %s " +
                    "WHERE %s IS NOT NULL GROUP BY %s ORDER BY count DESC LIMIT 10",
                    column, tableName, column, column
                );
                
                List<Map<String, Object>> topValues = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        Map<String, Object> value = new HashMap<>();
                        value.put("value", rs.getObject("value"));
                        value.put("count", rs.getLong("count"));
                        topValues.add(value);
                    }
                }
                
                dist.put("topValues", topValues);
                distributions.add(dist);
            }
        }
        
        return distributions;
    }

    private List<String> generateInsights(Map<String, Object> statistics, List<Map<String, Object>> distributions) {
        List<String> insights = new ArrayList<>();
        
        Long totalRows = (Long) statistics.get("totalRows");
        insights.add(String.format("数据表包含 %,d 条记录", totalRows));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columns = (List<Map<String, Object>>) statistics.get("columns");
        insights.add(String.format("共有 %d 个字段", columns.size()));
        
        // 分析数值列
        for (Map<String, Object> col : columns) {
            @SuppressWarnings("unchecked")
            Map<String, Object> colStats = (Map<String, Object>) col.get("statistics");
            if (colStats != null) {
                String colName = (String) col.get("name");
                Object min = colStats.get("min");
                Object max = colStats.get("max");
                Object avg = colStats.get("avg");
                
                if (min != null && max != null) {
                    insights.add(String.format("%s 的取值范围从 %s 到 %s，平均值为 %s", 
                        colName, min, max, avg));
                }
            }
        }
        
        return insights;
    }

    private String generateStoryText(String tableName, Map<String, Object> statistics, List<String> insights) {
        StringBuilder story = new StringBuilder();
        
        story.append("# ").append(tableName).append(" 数据故事\n\n");
        
        story.append("## 数据概览\n\n");
        Long totalRows = (Long) statistics.get("totalRows");
        Integer columnCount = (Integer) statistics.get("columnCount");
        story.append(String.format("该数据表共有 **%,d** 条记录，包含 **%d** 个字段。\n\n", 
            totalRows, columnCount));
        
        story.append("## 关键发现\n\n");
        for (int i = 0; i < insights.size(); i++) {
            story.append(i + 1).append(". ").append(insights.get(i)).append("\n");
        }
        
        story.append("\n## 数据洞察\n\n");
        story.append("基于以上数据分析，我们可以得出以下结论：\n\n");
        
        if (totalRows > 1000000) {
            story.append("- 这是一个大型数据集，包含超过百万条记录\n");
        } else if (totalRows > 10000) {
            story.append("- 这是一个中型数据集，数据量适中\n");
        } else {
            story.append("- 这是一个小型数据集，适合快速分析\n");
        }
        
        story.append("- 建议定期监控数据质量，确保数据准确性\n");
        story.append("- 可以使用这些数据来支持业务决策\n");
        
        return story.toString();
    }

    public Map<String, Object> generateComparisonStory(Long dataSourceId, String tableName, String compareColumn) {
        Map<String, Object> story = new HashMap<>();
        
        try {
            List<Map<String, Object>> comparisonData = new ArrayList<>();
            
            try (Connection connection = jdbcDataSource.getConnection();
                 Statement stmt = connection.createStatement()) {
                
                // 按时间段对比
                String sql = String.format(
                    "SELECT DATE(created_at) as date, COUNT(*) as count, " +
                    "SUM(amount) as total_amount FROM %s " +
                    "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                    "GROUP BY DATE(created_at) ORDER BY date",
                    tableName
                );
                
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("date", rs.getDate("date"));
                        data.put("count", rs.getLong("count"));
                        data.put("totalAmount", rs.getObject("total_amount"));
                        comparisonData.add(data);
                    }
                }
            }
            
            story.put("title", tableName + " 对比分析报告");
            story.put("comparisonData", comparisonData);
            story.put("generatedAt", new java.util.Date());
            
        } catch (Exception e) {
            story.put("error", e.getMessage());
        }
        
        return story;
    }
}
