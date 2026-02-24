package com.data.assistant.service;

import com.data.assistant.model.DataSource;
import com.data.assistant.repository.DataSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    
    @Autowired
    private DataSourceRepository dataSourceRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private DynamicDataSourceService dynamicDataSourceService;
    
    public List<Map<String, Object>> globalSearch(String keyword, List<String> types) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        if (types == null || types.isEmpty()) {
            types = Arrays.asList("table", "column", "report", "query");
        }
        
        for (DataSource ds : dataSourceRepository.findAll()) {
            try {
                dynamicDataSourceService.switchDataSource(ds.getId());
                
                if (types.contains("table")) {
                    results.addAll(searchTables(keyword, ds));
                }
                
                if (types.contains("column")) {
                    results.addAll(searchColumns(keyword, ds));
                }
            } catch (Exception e) {
                logger.warn("搜索数据源 {} 失败: {}", ds.getName(), e.getMessage());
            }
        }
        
        results.sort((a, b) -> {
            int scoreA = (Integer) a.getOrDefault("score", 0);
            int scoreB = (Integer) b.getOrDefault("score", 0);
            return scoreB - scoreA;
        });
        
        return results;
    }
    
    private List<Map<String, Object>> searchTables(String keyword, DataSource ds) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            String sql = "SELECT TABLE_NAME as name, TABLE_COMMENT as description, " +
                        "TABLE_ROWS as rowCount, TABLE_TYPE as tableType " +
                        "FROM INFORMATION_SCHEMA.TABLES " +
                        "WHERE TABLE_SCHEMA = DATABASE() " +
                        "AND (TABLE_NAME LIKE ? OR TABLE_COMMENT LIKE ?)";
            
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(sql, 
                "%" + keyword + "%", "%" + keyword + "%");
            
            for (Map<String, Object> table : tables) {
                Map<String, Object> result = new HashMap<>();
                result.put("id", ds.getId() + "_" + table.get("name"));
                result.put("type", "table");
                result.put("name", table.get("name"));
                result.put("description", table.get("description"));
                result.put("dataSourceId", ds.getId());
                result.put("dataSourceName", ds.getName());
                result.put("rowCount", table.get("rowCount"));
                result.put("score", calculateScore(keyword, (String) table.get("name"), (String) table.get("description")));
                result.put("createdAt", new Date());
                results.add(result);
            }
        } catch (Exception e) {
            logger.warn("搜索表失败: {}", e.getMessage());
        }
        
        return results;
    }
    
    private List<Map<String, Object>> searchColumns(String keyword, DataSource ds) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            String sql = "SELECT COLUMN_NAME as name, TABLE_NAME as tableName, " +
                        "COLUMN_TYPE as dataType, COLUMN_COMMENT as description " +
                        "FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() " +
                        "AND (COLUMN_NAME LIKE ? OR COLUMN_COMMENT LIKE ?)";
            
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(sql,
                "%" + keyword + "%", "%" + keyword + "%");
            
            for (Map<String, Object> column : columns) {
                Map<String, Object> result = new HashMap<>();
                result.put("id", ds.getId() + "_" + column.get("tableName") + "_" + column.get("name"));
                result.put("type", "column");
                result.put("name", column.get("name"));
                result.put("description", column.get("description"));
                result.put("tableName", column.get("tableName"));
                result.put("dataType", column.get("dataType"));
                result.put("dataSourceId", ds.getId());
                result.put("dataSourceName", ds.getName());
                result.put("score", calculateScore(keyword, (String) column.get("name"), (String) column.get("description")));
                result.put("createdAt", new Date());
                results.add(result);
            }
        } catch (Exception e) {
            logger.warn("搜索字段失败: {}", e.getMessage());
        }
        
        return results;
    }
    
    private int calculateScore(String keyword, String name, String description) {
        if (name == null) name = "";
        if (description == null) description = "";
        
        int score = 0;
        
        if (name.toLowerCase().equals(keyword.toLowerCase())) {
            score += 100;
        } else if (name.toLowerCase().startsWith(keyword.toLowerCase())) {
            score += 80;
        } else if (name.toLowerCase().contains(keyword.toLowerCase())) {
            score += 60;
        }
        
        if (description.toLowerCase().contains(keyword.toLowerCase())) {
            score += 20;
        }
        
        return score;
    }
    
    public List<String> getSearchSuggestions(String keyword) {
        List<String> suggestions = new ArrayList<>();
        
        if (keyword == null || keyword.length() < 2) {
            suggestions.add("销售");
            suggestions.add("订单");
            suggestions.add("客户");
            suggestions.add("产品");
            suggestions.add("库存");
            return suggestions;
        }
        
        try {
            for (DataSource ds : dataSourceRepository.findAll()) {
                dynamicDataSourceService.switchDataSource(ds.getId());
                
                List<String> tables = jdbcTemplate.queryForList(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME LIKE ? LIMIT 5",
                    String.class, "%" + keyword + "%");
                suggestions.addAll(tables);
            }
        } catch (Exception e) {
            logger.warn("获取搜索建议失败: {}", e.getMessage());
        }
        
        return suggestions.stream().distinct().limit(10).toList();
    }
}
