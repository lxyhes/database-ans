package com.data.assistant.controller;

import com.data.assistant.common.ApiResponse;
import com.data.assistant.service.HealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*")
public class HealthController {
    
    @Autowired
    private HealthService healthService;
    
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/overview/{dataSourceId}")
    public ResponseEntity<Map<String, Object>> getOverview(@PathVariable Long dataSourceId) {
        return ResponseEntity.ok(ApiResponse.success(
            healthService.getHealthOverview(dataSourceId)
        ));
    }
    
    @GetMapping("/table/{dataSourceId}/{tableName}")
    public ResponseEntity<Map<String, Object>> getTableHealth(
            @PathVariable Long dataSourceId,
            @PathVariable String tableName) {
        return ResponseEntity.ok(ApiResponse.success(
            healthService.getTableFieldHealth(dataSourceId, tableName)
        ));
    }
    
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> runHealthCheck(@RequestBody Map<String, Object> params) {
        Long dataSourceId = Long.valueOf(params.get("dataSourceId").toString());
        return ResponseEntity.ok(ApiResponse.success(
            healthService.getHealthOverview(dataSourceId)
        ));
    }
    
    @GetMapping("/trend/{dataSourceId}")
    public ResponseEntity<Map<String, Object>> getHealthTrend(@PathVariable Long dataSourceId) {
        return ResponseEntity.ok(ApiResponse.success(
            healthService.getHealthTrend(dataSourceId)
        ));
    }
}
