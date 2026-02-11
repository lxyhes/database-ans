package com.data.assistant.controller;

import com.data.assistant.service.SqlOptimizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/sql-optimization")
@CrossOrigin(origins = "*")
public class SqlOptimizationController {

    @Autowired
    private SqlOptimizationService sqlOptimizationService;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeSql(@RequestBody Map<String, String> request) {
        String sql = request.get("sql");
        if (sql == null || sql.isEmpty()) {
            return ResponseEntity.badRequest().body("SQL不能为空");
        }
        Map<String, Object> result = sqlOptimizationService.analyzeSql(sql);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/optimize")
    public ResponseEntity<?> optimizeSql(@RequestBody Map<String, String> request) {
        String sql = request.get("sql");
        if (sql == null || sql.isEmpty()) {
            return ResponseEntity.badRequest().body("SQL不能为空");
        }
        String optimized = sqlOptimizationService.optimizeSql(sql);
        Map<String, String> result = new HashMap<>();
        result.put("original", sql);
        result.put("optimized", optimized);
        return ResponseEntity.ok(result);
    }
}
