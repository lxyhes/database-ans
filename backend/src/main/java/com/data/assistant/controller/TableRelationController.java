package com.data.assistant.controller;

import com.data.assistant.model.TableRelation;
import com.data.assistant.service.TableRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/table-relations")
@CrossOrigin(origins = "*")
public class TableRelationController {

    @Autowired
    private TableRelationService tableRelationService;

    @PostMapping("/analyze/{dataSourceId}")
    public ResponseEntity<?> analyzeRelations(@PathVariable Long dataSourceId) {
        tableRelationService.analyzeTableRelations(dataSourceId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "分析完成"
        ));
    }

    @GetMapping("/{dataSourceId}")
    public ResponseEntity<?> getRelations(@PathVariable Long dataSourceId) {
        List<TableRelation> relations = tableRelationService.getTableRelations(dataSourceId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", relations
        ));
    }

    @GetMapping("/{dataSourceId}/graph")
    public ResponseEntity<?> getRelationGraph(@PathVariable Long dataSourceId) {
        Map<String, Object> graphData = tableRelationService.getRelationGraphData(dataSourceId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", graphData
        ));
    }

    @GetMapping("/{dataSourceId}/table/{tableName}")
    public ResponseEntity<?> getTableRelations(@PathVariable Long dataSourceId, @PathVariable String tableName) {
        List<TableRelation> relations = tableRelationService.getTableRelationsForTable(dataSourceId, tableName);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", relations
        ));
    }

    @PostMapping
    public ResponseEntity<?> addManualRelation(@RequestBody Map<String, Object> request) {
        Long dataSourceId = Long.valueOf(request.get("dataSourceId").toString());
        String sourceTable = (String) request.get("sourceTable");
        String sourceColumn = (String) request.get("sourceColumn");
        String targetTable = (String) request.get("targetTable");
        String targetColumn = (String) request.get("targetColumn");
        String description = (String) request.get("description");

        TableRelation relation = tableRelationService.addManualRelation(
                dataSourceId, sourceTable, sourceColumn, targetTable, targetColumn, description);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", relation
        ));
    }

    @DeleteMapping("/{relationId}")
    public ResponseEntity<?> deleteRelation(@PathVariable Long relationId) {
        tableRelationService.deleteRelation(relationId);
        return ResponseEntity.ok(Map.of(
            "success", true
        ));
    }
}
