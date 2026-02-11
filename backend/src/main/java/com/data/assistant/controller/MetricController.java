package com.data.assistant.controller;

import com.data.assistant.common.ApiResponse;
import com.data.assistant.model.Metric;
import com.data.assistant.service.MetricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "*")
public class MetricController {

    @Autowired
    private MetricService metricService;

    @PostMapping
    public ResponseEntity<?> createMetric(@RequestBody Metric metric) {
        try {
            Metric created = metricService.createMetric(metric);
            return ResponseEntity.ok(ApiResponse.success(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMetric(@PathVariable Long id, @RequestBody Metric metric) {
        try {
            Metric updated = metricService.updateMetric(id, metric);
            return ResponseEntity.ok(ApiResponse.success(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllMetrics() {
        List<Metric> metrics = metricService.getAllMetrics();
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMetric(@PathVariable Long id) {
        try {
            Metric metric = metricService.getMetric(id);
            return ResponseEntity.ok(ApiResponse.success(metric));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMetric(@PathVariable Long id) {
        metricService.deleteMetric(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/calculate")
    public ResponseEntity<?> calculateMetric(@PathVariable Long id) {
        try {
            Map<String, Object> result = metricService.calculateMetric(id);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("计算失败: " + e.getMessage()));
        }
    }

    @GetMapping("/by-category")
    public ResponseEntity<?> getMetricsByCategory() {
        Map<String, List<Metric>> grouped = metricService.getMetricsByCategory();
        return ResponseEntity.ok(ApiResponse.success(grouped));
    }

    @GetMapping("/category/{category}/calculate")
    public ResponseEntity<?> calculateMetricsByCategory(@PathVariable String category) {
        List<Map<String, Object>> results = metricService.calculateMetricsByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/{id}/trend")
    public ResponseEntity<?> getMetricTrend(@PathVariable Long id,
                                            @RequestParam String timeField,
                                            @RequestParam(defaultValue = "30") int days) {
        try {
            List<Map<String, Object>> trend = metricService.getMetricTrend(id, timeField, days);
            return ResponseEntity.ok(ApiResponse.success(trend));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("获取趋势失败: " + e.getMessage()));
        }
    }
}
