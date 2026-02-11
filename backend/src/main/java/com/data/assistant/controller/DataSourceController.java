package com.data.assistant.controller;

import com.data.assistant.model.DataSource;
import com.data.assistant.service.DataSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据源管理控制器
 */
@RestController
@RequestMapping("/api/datasources")
@CrossOrigin(origins = "*")
public class DataSourceController {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSourceController.class);
    
    @Autowired
    private DataSourceService dataSourceService;
    
    /**
     * 获取所有数据源
     */
    @GetMapping
    public ResponseEntity<?> getAllDataSources() {
        try {
            List<DataSource> sources = dataSourceService.getAllDataSources();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", sources
            ));
        } catch (Exception e) {
            logger.error("Failed to get data sources", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 根据 ID 获取数据源
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDataSourceById(@PathVariable Long id) {
        try {
            DataSource source = dataSourceService.getDataSourceById(id)
                .orElseThrow(() -> new IllegalArgumentException("数据源不存在"));
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", source
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 创建数据源
     */
    @PostMapping
    public ResponseEntity<?> createDataSource(@RequestBody DataSource dataSource) {
        try {
            DataSource created = dataSourceService.createDataSource(dataSource);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", created,
                "message", "数据源创建成功"
            ));
        } catch (Exception e) {
            logger.error("Failed to create data source", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 更新数据源
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDataSource(@PathVariable Long id, @RequestBody DataSource dataSource) {
        try {
            DataSource updated = dataSourceService.updateDataSource(id, dataSource);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", updated,
                "message", "数据源更新成功"
            ));
        } catch (Exception e) {
            logger.error("Failed to update data source", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 删除数据源
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDataSource(@PathVariable Long id) {
        try {
            dataSourceService.deleteDataSource(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "数据源删除成功"
            ));
        } catch (Exception e) {
            logger.error("Failed to delete data source", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 测试数据源连接
     */
    @PostMapping("/test")
    public ResponseEntity<?> testConnection(@RequestBody DataSource dataSource) {
        try {
            boolean connected = dataSourceService.testConnection(dataSource);
            if (connected) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "连接成功"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "连接失败"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "连接测试失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 设置默认数据源
     */
    @PutMapping("/{id}/default")
    public ResponseEntity<?> setDefaultDataSource(@PathVariable Long id) {
        try {
            dataSourceService.setDefaultDataSource(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "默认数据源设置成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 获取支持的数据库类型
     */
    @GetMapping("/types")
    public ResponseEntity<?> getSupportedTypes() {
        Map<String, Object> types = new HashMap<>();
        
        types.put("mysql", Map.of(
            "name", "MySQL",
            "defaultPort", 3306,
            "driver", "com.mysql.cj.jdbc.Driver"
        ));
        
        types.put("postgresql", Map.of(
            "name", "PostgreSQL",
            "defaultPort", 5432,
            "driver", "org.postgresql.Driver"
        ));
        
        types.put("h2", Map.of(
            "name", "H2 Database",
            "defaultPort", 0,
            "driver", "org.h2.Driver"
        ));
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", types
        ));
    }
    
    /**
     * 获取数据源的所有表
     */
    @GetMapping("/{id}/tables")
    public ResponseEntity<?> getDataSourceTables(@PathVariable Long id) {
        try {
            List<Map<String, Object>> tables = dataSourceService.getDataSourceTables(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", tables
            ));
        } catch (Exception e) {
            logger.error("Failed to get tables for datasource {}", id, e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 获取表的列信息
     */
    @GetMapping("/{id}/tables/{tableName}/columns")
    public ResponseEntity<?> getTableColumns(
            @PathVariable Long id,
            @PathVariable String tableName) {
        try {
            List<Map<String, Object>> columns = dataSourceService.getTableColumns(id, tableName);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", columns
            ));
        } catch (Exception e) {
            logger.error("Failed to get columns for table {} in datasource {}", tableName, id, e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
