package com.data.assistant.controller;

import com.data.assistant.model.QueryResponse;
import com.data.assistant.service.DataQueryService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public String getDataSummary() {
        return dataQueryService.getDataSummary();
    }

    /**
     * 获取表结构
     */
    @GetMapping("/schema/{tableName}")
    public Map<String, Object> getTableSchema(@PathVariable String tableName) {
        return dataQueryService.getTableSchema(tableName);
    }

    /**
     * 获取所有表名
     */
    @GetMapping("/tables")
    public List<String> getAllTables() {
        return dataQueryService.getAllTableNames();
    }

    /**
     * 获取样本数据
     */
    @GetMapping("/sample/{tableName}")
    public List<Map<String, Object>> getSampleData(@PathVariable String tableName, @RequestParam(defaultValue = "5") int limit) {
        return dataQueryService.getSampleData(tableName, limit);
    }
}