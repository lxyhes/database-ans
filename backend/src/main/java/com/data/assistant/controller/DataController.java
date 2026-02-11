package com.data.assistant.controller;

import com.data.assistant.model.QueryResponse;
import com.data.assistant.service.DataQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据管理控制器
 * 提供数据摘要、表结构等 API
 */
@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = "*")
public class DataController {

    @Autowired
    private DataQueryService dataQueryService;

    /**
     * 获取数据摘要
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getDataSummary() {
        String summary = dataQueryService.getDataSummary();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", summary
        ));
    }

    /**
     * 获取表结构
     */
    @GetMapping("/schema/{tableName}")
    public ResponseEntity<?> getTableSchema(@PathVariable String tableName) {
        Map<String, Object> schema = dataQueryService.getTableSchema(tableName);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", schema
        ));
    }

    /**
     * 获取所有表名
     */
    @GetMapping("/tables")
    public ResponseEntity<?> getAllTables(@RequestParam(required = false) Long dataSourceId) {
        if (dataSourceId != null) {
            dataQueryService.switchDataSource(dataSourceId);
        }
        List<String> tables = dataQueryService.getAllTableNames();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", tables
        ));
    }

    /**
     * 获取样本数据
     */
    @GetMapping("/sample/{tableName}")
    public ResponseEntity<?> getSampleData(@PathVariable String tableName, @RequestParam(defaultValue = "5") int limit) {
        List<Map<String, Object>> sampleData = dataQueryService.getSampleData(tableName, limit);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", sampleData
        ));
    }
}
