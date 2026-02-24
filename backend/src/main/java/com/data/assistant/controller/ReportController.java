package com.data.assistant.controller;

import com.data.assistant.common.ApiResponse;
import com.data.assistant.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {
    
    @Autowired
    private ReportService reportService;
    
    @GetMapping("/templates")
    public ResponseEntity<Map<String, Object>> getTemplates() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getAllTemplates()));
    }
    
    @PostMapping("/templates")
    public ResponseEntity<Map<String, Object>> createTemplate(@RequestBody Map<String, Object> template) {
        return ResponseEntity.ok(ApiResponse.success(reportService.createTemplate(null)));
    }
    
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateReport(@RequestBody Map<String, Object> params) {
        try {
            Long dataSourceId = Long.valueOf(params.get("dataSourceId").toString());
            List<String> tableNames = (List<String>) params.get("tableNames");
            String title = (String) params.get("title");
            List<String> dimensions = (List<String>) params.get("dimensions");
            Long templateId = params.get("templateId") != null ? 
                Long.valueOf(params.get("templateId").toString()) : null;
            
            return ResponseEntity.ok(ApiResponse.success(
                reportService.generateReport(dataSourceId, tableNames, title, dimensions, null, templateId)
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("生成报告失败: " + e.getMessage()));
        }
    }
    
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getReportHistory(@RequestParam(required = false) Long dataSourceId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getReportHistory(dataSourceId)));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getReport(@PathVariable Long id) {
        return reportService.getReportById(id)
            .map(r -> ResponseEntity.ok(ApiResponse.success(r)))
            .orElse(ResponseEntity.ok(ApiResponse.error("报告不存在")));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteReport(@PathVariable Long id) {
        reportService.deleteReport(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功"));
    }
}
