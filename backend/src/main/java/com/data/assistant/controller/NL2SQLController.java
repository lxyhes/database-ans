package com.data.assistant.controller;

import com.data.assistant.service.ai.*;
import com.data.assistant.service.DataQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NL2SQL 控制器
 * 自然语言转 SQL 查询
 */
@RestController
@RequestMapping("/api/nl2sql")
@CrossOrigin(origins = "*")
public class NL2SQLController {
    
    private static final Logger logger = LoggerFactory.getLogger(NL2SQLController.class);
    
    @Autowired
    private UnifiedAIService unifiedAIService;
    
    @Autowired
    private DataQueryService dataQueryService;
    
    /**
     * 自然语言转 SQL
     */
    @PostMapping("/convert")
    public ResponseEntity<?> naturalLanguageToSQL(
            @RequestBody NL2SQLRequest request) {
        
        try {
            logger.info("NL2SQL request: {}, datasource: {}", request.getQuery(), request.getDataSourceId());
            
            // 切换数据源（如果指定）
            if (request.getDataSourceId() != null) {
                dataQueryService.switchDataSource(request.getDataSourceId());
            }
            
            // 获取数据库 schema
            String schema = getDatabaseSchema();
            
            // 构建上下文（支持多轮对话）
            Map<String, Object> context = new HashMap<>();
            if (request.getHistory() != null && !request.getHistory().isEmpty()) {
                // 将历史记录列表转换为字符串格式
                StringBuilder historyBuilder = new StringBuilder();
                for (Map<String, Object> msg : request.getHistory()) {
                    String role = msg.get("role") != null ? msg.get("role").toString() : "";
                    String content = msg.get("content") != null ? msg.get("content").toString() : "";
                    historyBuilder.append(role).append(": ").append(content).append("\n");
                }
                context.put("history", historyBuilder.toString());
            }
            context.put("dataSourceId", dataQueryService.getCurrentDataSourceId());
            
            // 调用 AI 服务
            NL2SQLResult result = unifiedAIService.naturalLanguageToSQL(
                request.getProvider(),
                request.getQuery(),
                schema,
                context
            );
            
            if (result.isSuccess()) {
                // 执行 SQL 获取数据，并记录耗时
                long startTime = System.currentTimeMillis();
                List<Map<String, Object>> data = dataQueryService.executeQuery(result.getSql());
                long executionTime = System.currentTimeMillis() - startTime;
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("sql", result.getSql());
                response.put("data", data);
                response.put("intent", result.getIntent());
                response.put("description", result.getDescription());
                response.put("suggestedChart", result.getSuggestedChart());
                response.put("dataSourceId", dataQueryService.getCurrentDataSourceId());
                response.put("executionTime", executionTime);
                response.put("rowCount", data != null ? data.size() : 0);
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", result.getErrorMessage()
                ));
            }
        } catch (Exception e) {
            logger.error("NL2SQL error", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "转换失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 智能查询（自动执行 SQL 并返回结果）
     */
    @PostMapping("/query")
    public ResponseEntity<?> smartQuery(@RequestBody NL2SQLRequest request) {
        return naturalLanguageToSQL(request);
    }
    
    /**
     * 解释 SQL
     */
    @PostMapping("/explain")
    public ResponseEntity<?> explainSQL(@RequestBody Map<String, String> request) {
        try {
            String sql = request.get("sql");
            String provider = request.getOrDefault("provider", "iflow");
            
            String prompt = "请用中文解释以下 SQL 语句的作用：\n\n" + sql;
            String explanation = unifiedAIService.chat(provider, prompt, null);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "explanation", explanation
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "解释失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 优化 SQL
     */
    @PostMapping("/optimize")
    public ResponseEntity<?> optimizeSQL(@RequestBody Map<String, String> request) {
        try {
            String sql = request.get("sql");
            String provider = request.getOrDefault("provider", "iflow");
            
            String prompt = "请优化以下 SQL 语句，提高性能和可读性，并解释优化建议：\n\n" + sql;
            String optimization = unifiedAIService.chat(provider, prompt, null);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "optimization", optimization
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "优化失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取数据库 schema 信息
     */
    private String getDatabaseSchema() {
        try {
            List<String> tables = dataQueryService.getAllTableNames();
            StringBuilder schema = new StringBuilder();
            
            for (String table : tables) {
                schema.append("表名: ").append(table).append("\n");
                
                // 获取表字段信息
                try {
                    Map<String, Object> tableSchema = dataQueryService.getTableSchema(table);
                    List<Map<String, Object>> columns = (List<Map<String, Object>>) tableSchema.get("columns");
                    if (columns != null && !columns.isEmpty()) {
                        schema.append("  字段:\n");
                        for (Map<String, Object> col : columns) {
                            String colName = col.get("COLUMN_NAME") != null ? col.get("COLUMN_NAME").toString() : "unknown";
                            String dataType = col.get("DATA_TYPE") != null ? col.get("DATA_TYPE").toString() : "unknown";
                            schema.append("    - ").append(colName).append(" (").append(dataType).append(")\n");
                        }
                    }
                } catch (Exception ex) {
                    logger.warn("Failed to get schema for table: {}", table, ex);
                }
                schema.append("\n");
            }
            
            return schema.toString();
        } catch (Exception e) {
            logger.error("Failed to get schema", e);
            return "";
        }
    }
    
    /**
     * NL2SQL 请求
     */
    public static class NL2SQLRequest {
        private String query;
        private String provider;
        private List<Map<String, Object>> history;
        private Long dataSourceId;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public List<Map<String, Object>> getHistory() {
            return history;
        }

        public void setHistory(List<Map<String, Object>> history) {
            this.history = history;
        }

        public Long getDataSourceId() {
            return dataSourceId;
        }

        public void setDataSourceId(Long dataSourceId) {
            this.dataSourceId = dataSourceId;
        }
    }
}
