package com.data.assistant.controller;

import com.data.assistant.common.ApiResponse;
import com.data.assistant.service.DataStoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/data-story")
@CrossOrigin(origins = "*")
public class DataStoryController {

    @Autowired
    private DataStoryService dataStoryService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateDataStory(@RequestBody Map<String, Object> request) {
        try {
            Long dataSourceId = Long.valueOf(request.get("dataSourceId").toString());
            String tableName = (String) request.get("tableName");
            
            Map<String, Object> story = dataStoryService.generateDataStory(dataSourceId, tableName);
            return ResponseEntity.ok(ApiResponse.success(story));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("生成失败: " + e.getMessage()));
        }
    }

    @PostMapping("/compare")
    public ResponseEntity<?> generateComparisonStory(@RequestBody Map<String, Object> request) {
        try {
            Long dataSourceId = Long.valueOf(request.get("dataSourceId").toString());
            String tableName = (String) request.get("tableName");
            String compareColumn = (String) request.get("compareColumn");
            
            Map<String, Object> story = dataStoryService.generateComparisonStory(dataSourceId, tableName, compareColumn);
            return ResponseEntity.ok(ApiResponse.success(story));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("生成失败: " + e.getMessage()));
        }
    }
}
