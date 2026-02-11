package com.data.assistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据查询服务
 * 执行 SQL 查询并返回结果（支持动态数据源）
 */
@Service
public class DataQueryService {
    private static final Logger logger = LoggerFactory.getLogger(DataQueryService.class);

    @Autowired
    private DynamicDataSourceService dynamicDataSourceService;

    /**
     * 清理 SQL（移除 Markdown 代码块标记等）
     */
    private String cleanSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }
        // 移除 Markdown 代码块标记
        sql = sql.replaceAll("```sql", "");
        sql = sql.replaceAll("```", "");
        // 移除前后空白
        sql = sql.trim();
        return sql;
    }

    /**
     * 执行 SQL 查询（使用当前数据源）
     */
    public List<Map<String, Object>> executeQuery(String sql) {
        try {
            sql = cleanSql(sql);
            logger.info("Executing SQL query: {}", sql);
            JdbcTemplate jdbcTemplate = dynamicDataSourceService.getCurrentJdbcTemplate();
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            logger.info("Query returned {} rows", results.size());
            return results;
        } catch (Exception e) {
            logger.error("Failed to execute SQL query: {}", sql, e);
            throw new RuntimeException("查询执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行 SQL 查询（指定数据源）
     */
    public List<Map<String, Object>> executeQuery(Long dataSourceId, String sql) {
        try {
            sql = cleanSql(sql);
            logger.info("Executing SQL query on datasource {}: {}", dataSourceId, sql);
            JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate(dataSourceId);
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            logger.info("Query returned {} rows", results.size());
            return results;
        } catch (Exception e) {
            logger.error("Failed to execute SQL query on datasource {}: {}", dataSourceId, sql, e);
            throw new RuntimeException("查询执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行分析查询
     */
    public List<Map<String, Object>> executeAnalysisQuery(String type, String sql) {
        return executeQuery(sql);
    }

    /**
     * 获取数据库表结构信息（使用当前数据源）
     */
    public Map<String, Object> getTableSchema(String tableName) {
        try {
            JdbcTemplate jdbcTemplate = dynamicDataSourceService.getCurrentJdbcTemplate();
            String sql = String.format(
                    "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_KEY " +
                            "FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_NAME = '%s'",
                    tableName.toUpperCase()
            );
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(sql);
            return Map.of("tableName", tableName, "columns", columns);
        } catch (Exception e) {
            logger.error("Failed to get table schema for: {}", tableName, e);
            return Map.of("tableName", tableName, "columns", new ArrayList<>());
        }
    }

    /**
     * 获取所有表名（使用当前数据源）
     */
    public List<String> getAllTableNames() {
        List<String> tables = new ArrayList<>();
        try {
            JdbcTemplate jdbcTemplate = dynamicDataSourceService.getCurrentJdbcTemplate();

            // 方式1: MySQL - SHOW TABLES
            try {
                tables = jdbcTemplate.queryForList("SHOW TABLES", String.class);
                if (!tables.isEmpty()) {
                    return tables;
                }
            } catch (Exception ex) {
                logger.debug("MySQL SHOW TABLES failed, trying next method");
            }

            // 方式2: PostgreSQL
            try {
                tables = jdbcTemplate.queryForList(
                    "SELECT tablename FROM pg_tables WHERE schemaname = 'public'", String.class);
                if (!tables.isEmpty()) {
                    return tables;
                }
            } catch (Exception ex) {
                logger.debug("PostgreSQL query failed, trying next method");
            }

            // 方式3: H2
            try {
                tables = jdbcTemplate.queryForList(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'", String.class);
                if (!tables.isEmpty()) {
                    return tables;
                }
            } catch (Exception ex) {
                logger.debug("H2 query failed, trying next method");
            }

            // 方式4: 通用 INFORMATION_SCHEMA
            try {
                tables = jdbcTemplate.queryForList(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'", String.class);
            } catch (Exception ex) {
                logger.error("All methods failed to get table names", ex);
            }

        } catch (Exception e) {
            logger.error("Failed to get table names", e);
        }
        return tables;
    }

    /**
     * 获取数据摘要用于 AI 分析（使用当前数据源）
     */
    public String getDataSummary() {
        try {
            JdbcTemplate jdbcTemplate = dynamicDataSourceService.getCurrentJdbcTemplate();
            StringBuilder summary = new StringBuilder();
            summary.append("数据库包含以下表和数据概览：\n\n");

            List<String> tables = getAllTableNames();
            for (String table : tables) {
                summary.append(String.format("表名: %s\n", table));

                // 获取行数
                try {
                    String countSql = String.format("SELECT COUNT(*) FROM %s", table);
                    Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
                    summary.append(String.format("  总记录数: %d\n", count));
                } catch (Exception e) {
                    summary.append("  总记录数: 无法获取\n");
                }

                // 获取表结构
                Map<String, Object> schema = getTableSchema(table);
                List<Map<String, Object>> columns = (List<Map<String, Object>>) schema.get("columns");
                summary.append("  字段: ");
                for (int i = 0; i < columns.size(); i++) {
                    Map<String, Object> col = columns.get(i);
                    summary.append(col.get("COLUMN_NAME"));
                    if (i < columns.size() - 1) {
                        summary.append(", ");
                    }
                }
                summary.append("\n\n");
            }

            return summary.toString();
        } catch (Exception e) {
            logger.error("Failed to generate data summary", e);
            return "无法获取数据摘要: " + e.getMessage();
        }
    }

    /**
     * 获取样本数据（使用当前数据源）
     */
    public List<Map<String, Object>> getSampleData(String tableName, int limit) {
        try {
            JdbcTemplate jdbcTemplate = dynamicDataSourceService.getCurrentJdbcTemplate();
            String sql = String.format("SELECT * FROM %s LIMIT %d", tableName, limit);
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            logger.error("Failed to get sample data from: {}", tableName, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取当前数据源ID
     */
    public Long getCurrentDataSourceId() {
        return dynamicDataSourceService.getCurrentDataSourceId();
    }

    /**
     * 切换数据源
     */
    public void switchDataSource(Long dataSourceId) {
        dynamicDataSourceService.switchDataSource(dataSourceId);
    }
}
