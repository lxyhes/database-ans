package com.data.assistant.controller;

import com.data.assistant.common.ApiResponse;
import com.data.assistant.model.DataLineage;
import com.data.assistant.service.DataLineageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/lineage")
@CrossOrigin(origins = "*")
public class DataLineageController {

    @Autowired
    private DataLineageService dataLineageService;

    @PostMapping
    public ResponseEntity<?> addLineage(@RequestBody DataLineage lineage) {
        DataLineage created = dataLineageService.addManualLineage(lineage);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @GetMapping("/{dataSourceId}")
    public ResponseEntity<?> getLineages(@PathVariable Long dataSourceId) {
        List<DataLineage> lineages = dataLineageService.getLineages(dataSourceId);
        return ResponseEntity.ok(ApiResponse.success(lineages));
    }

    @GetMapping("/{dataSourceId}/table/{tableName}")
    public ResponseEntity<?> getTableLineages(@PathVariable Long dataSourceId, @PathVariable String tableName) {
        List<DataLineage> lineages = dataLineageService.getTableLineages(dataSourceId, tableName);
        return ResponseEntity.ok(ApiResponse.success(lineages));
    }

    @GetMapping("/{dataSourceId}/graph")
    public ResponseEntity<?> getLineageGraph(@PathVariable Long dataSourceId, @RequestParam String tableName) {
        Map<String, Object> graphData = dataLineageService.getLineageGraph(dataSourceId, tableName);
        return ResponseEntity.ok(ApiResponse.success(graphData));
    }

    @GetMapping("/{dataSourceId}/upstream")
    public ResponseEntity<?> traceUpstream(@PathVariable Long dataSourceId, 
                                           @RequestParam String tableName,
                                           @RequestParam String columnName) {
        List<Map<String, Object>> upstream = dataLineageService.traceUpstream(dataSourceId, tableName, columnName);
        return ResponseEntity.ok(ApiResponse.success(upstream));
    }

    @GetMapping("/{dataSourceId}/downstream")
    public ResponseEntity<?> traceDownstream(@PathVariable Long dataSourceId,
                                             @RequestParam String tableName,
                                             @RequestParam String columnName) {
        List<Map<String, Object>> downstream = dataLineageService.traceDownstream(dataSourceId, tableName, columnName);
        return ResponseEntity.ok(ApiResponse.success(downstream));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLineage(@PathVariable Long id) {
        dataLineageService.deleteLineage(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
