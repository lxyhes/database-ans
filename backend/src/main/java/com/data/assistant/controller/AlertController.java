package com.data.assistant.controller;

import com.data.assistant.common.ApiResponse;
import com.data.assistant.model.AlertRule;
import com.data.assistant.model.AlertRecord;
import com.data.assistant.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertController {
    
    @Autowired
    private AlertService alertService;
    
    @GetMapping("/rules")
    public ResponseEntity<Map<String, Object>> getAllRules() {
        List<AlertRule> rules = alertService.getAllRules();
        return ResponseEntity.ok(ApiResponse.success(rules));
    }
    
    @GetMapping("/rules/active")
    public ResponseEntity<Map<String, Object>> getActiveRules() {
        List<AlertRule> rules = alertService.getActiveRules();
        return ResponseEntity.ok(ApiResponse.success(rules));
    }
    
    @GetMapping("/rules/{id}")
    public ResponseEntity<Map<String, Object>> getRuleById(@PathVariable Long id) {
        return alertService.getRuleById(id)
            .map(rule -> ResponseEntity.ok(ApiResponse.success(rule)))
            .orElse(ResponseEntity.ok(ApiResponse.error("规则不存在")));
    }
    
    @PostMapping("/rules")
    public ResponseEntity<Map<String, Object>> createRule(@RequestBody AlertRule rule) {
        try {
            AlertRule created = alertService.createRule(rule);
            return ResponseEntity.ok(ApiResponse.success(created));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("创建失败: " + e.getMessage()));
        }
    }
    
    @PutMapping("/rules/{id}")
    public ResponseEntity<Map<String, Object>> updateRule(@PathVariable Long id, @RequestBody AlertRule rule) {
        try {
            AlertRule updated = alertService.updateRule(id, rule);
            return ResponseEntity.ok(ApiResponse.success(updated));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("更新失败: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Map<String, Object>> deleteRule(@PathVariable Long id) {
        try {
            alertService.deleteRule(id);
            return ResponseEntity.ok(ApiResponse.success("删除成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("删除失败: " + e.getMessage()));
        }
    }
    
    @PutMapping("/rules/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleRule(@PathVariable Long id, @RequestParam boolean active) {
        try {
            alertService.toggleRule(id, active);
            return ResponseEntity.ok(ApiResponse.success(active ? "已启用" : "已禁用"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("操作失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/rules/{id}/check")
    public ResponseEntity<Map<String, Object>> manualCheck(@PathVariable Long id) {
        try {
            alertService.manualCheck(id);
            return ResponseEntity.ok(ApiResponse.success("检查完成"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("检查失败: " + e.getMessage()));
        }
    }
    
    @GetMapping("/records")
    public ResponseEntity<Map<String, Object>> getAlertRecords(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "100") int limit) {
        List<AlertRecord> records = alertService.getAlertRecords(status, level, limit);
        return ResponseEntity.ok(ApiResponse.success(records));
    }
    
    @GetMapping("/records/recent")
    public ResponseEntity<Map<String, Object>> getRecentAlerts(@RequestParam(defaultValue = "24") int hours) {
        List<AlertRecord> records = alertService.getRecentAlerts(hours);
        return ResponseEntity.ok(ApiResponse.success(records));
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = alertService.getAlertStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    @PutMapping("/records/{id}/confirm")
    public ResponseEntity<Map<String, Object>> confirmAlert(
            @PathVariable Long id,
            @RequestParam(required = false) String confirmedBy) {
        try {
            alertService.confirmAlert(id, confirmedBy != null ? confirmedBy : "system");
            return ResponseEntity.ok(ApiResponse.success("已确认"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("确认失败: " + e.getMessage()));
        }
    }
    
    @PutMapping("/records/{id}/resolve")
    public ResponseEntity<Map<String, Object>> resolveAlert(
            @PathVariable Long id,
            @RequestParam(required = false) String resolvedBy,
            @RequestParam(required = false) String note) {
        try {
            alertService.resolveAlert(id, 
                resolvedBy != null ? resolvedBy : "system", 
                note != null ? note : "");
            return ResponseEntity.ok(ApiResponse.success("已解决"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("解决失败: " + e.getMessage()));
        }
    }
    
    @PutMapping("/records/{id}/ignore")
    public ResponseEntity<Map<String, Object>> ignoreAlert(@PathVariable Long id) {
        try {
            alertService.ignoreAlert(id);
            return ResponseEntity.ok(ApiResponse.success("已忽略"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("操作失败: " + e.getMessage()));
        }
    }
}
