package com.data.assistant.controller;

import com.data.assistant.service.FederationQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/federation")
@CrossOrigin(origins = "*")
public class FederationQueryController {

    @Autowired
    private FederationQueryService federationQueryService;

    @PostMapping("/query")
    public ResponseEntity<?> executeFederationQuery(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> dataSourceIds = (List<Long>) request.get("dataSourceIds");
            String sql = (String) request.get("sql");
            
            Map<String, Object> result = federationQueryService.executeFederationQuery(dataSourceIds, sql);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinAcrossDataSources(@RequestBody Map<String, Object> request) {
        try {
            Long leftDsId = Long.valueOf(request.get("leftDsId").toString());
            String leftTable = (String) request.get("leftTable");
            Long rightDsId = Long.valueOf(request.get("rightDsId").toString());
            String rightTable = (String) request.get("rightTable");
            String joinColumn = (String) request.get("joinColumn");
            
            Map<String, Object> result = federationQueryService.joinAcrossDataSources(
                leftDsId, leftTable, rightDsId, rightTable, joinColumn);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("JOIN失败: " + e.getMessage());
        }
    }

    @PostMapping("/aggregate")
    public ResponseEntity<?> aggregateAcrossDataSources(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> dataSourceIds = (List<Long>) request.get("dataSourceIds");
            String tableName = (String) request.get("tableName");
            String aggColumn = (String) request.get("aggColumn");
            String aggFunction = (String) request.get("aggFunction");
            
            List<Map<String, Object>> results = federationQueryService.aggregateAcrossDataSources(
                dataSourceIds, tableName, aggColumn, aggFunction);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("聚合失败: " + e.getMessage());
        }
    }
}
