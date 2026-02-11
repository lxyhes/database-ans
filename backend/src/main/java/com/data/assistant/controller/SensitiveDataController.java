package com.data.assistant.controller;

import com.data.assistant.model.SensitiveColumn;
import com.data.assistant.service.SensitiveDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/sensitive-data")
@CrossOrigin(origins = "*")
public class SensitiveDataController {

    @Autowired
    private SensitiveDataService sensitiveDataService;

    @PostMapping("/scan/{dataSourceId}")
    public ResponseEntity<?> scanTable(
            @PathVariable Long dataSourceId,
            @RequestBody Map<String, String> request) {
        String tableName = request.get("tableName");
        sensitiveDataService.scanTable(dataSourceId, tableName);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{dataSourceId}")
    public ResponseEntity<?> getSensitiveColumns(@PathVariable Long dataSourceId) {
        List<SensitiveColumn> columns = sensitiveDataService.getSensitiveColumns(dataSourceId);
        return ResponseEntity.ok(columns);
    }

    @GetMapping("/{dataSourceId}/table/{tableName}")
    public ResponseEntity<?> getTableSensitiveColumns(
            @PathVariable Long dataSourceId,
            @PathVariable String tableName) {
        List<SensitiveColumn> columns = sensitiveDataService.getSensitiveColumns(dataSourceId, tableName);
        return ResponseEntity.ok(columns);
    }

    @GetMapping("/{dataSourceId}/overview")
    public ResponseEntity<?> getOverview(@PathVariable Long dataSourceId) {
        Map<String, Object> overview = sensitiveDataService.getSensitiveDataOverview(dataSourceId);
        return ResponseEntity.ok(overview);
    }

    @PutMapping("/config/{columnId}")
    public ResponseEntity<?> updateMaskConfig(
            @PathVariable Long columnId,
            @RequestBody Map<String, Object> request) {
        Boolean isMasked = (Boolean) request.get("isMasked");
        String maskRule = (String) request.get("maskRule");
        sensitiveDataService.updateMaskConfig(columnId, isMasked, maskRule);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{columnId}")
    public ResponseEntity<?> deleteSensitiveColumn(@PathVariable Long columnId) {
        sensitiveDataService.deleteSensitiveColumn(columnId);
        return ResponseEntity.ok().build();
    }
}
