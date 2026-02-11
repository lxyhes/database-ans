package com.data.assistant.controller;

import com.data.assistant.common.ApiResponse;
import com.data.assistant.model.QueryHistory;
import com.data.assistant.service.QueryHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 查询历史控制器
 */
@RestController
@RequestMapping("/api/query-history")
@CrossOrigin(origins = "*")
public class QueryHistoryController {

    @Autowired
    private QueryHistoryService queryHistoryService;

    /**
     * 获取所有查询历史
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllQueries() {
        return ResponseEntity.ok(ApiResponse.success(queryHistoryService.getAllQueries()));
    }

    /**
     * 获取收藏的查询
     */
    @GetMapping("/favorites")
    public ResponseEntity<?> getFavoriteQueries() {
        return ResponseEntity.ok(ApiResponse.success(queryHistoryService.getFavoriteQueries()));
    }

    /**
     * 保存查询
     */
    @PostMapping("/save")
    public ResponseEntity<?> saveQuery(@RequestBody Map<String, String> request) {
        String queryText = request.get("queryText");
        String sqlQuery = request.get("sqlQuery");
        String queryType = request.getOrDefault("queryType", "general");
        String tags = request.get("tags");
        QueryHistory saved = queryHistoryService.saveQuery(queryText, sqlQuery, queryType, tags);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    /**
     * 切换收藏状态
     */
    @PostMapping("/{id}/toggle-favorite")
    public ResponseEntity<?> toggleFavorite(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(queryHistoryService.toggleFavorite(id)));
    }

    /**
     * 删除查询
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuery(@PathVariable Long id) {
        queryHistoryService.deleteQuery(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功"));
    }

    /**
     * 根据类型查询
     */
    @GetMapping("/type/{queryType}")
    public ResponseEntity<?> getQueriesByType(@PathVariable String queryType) {
        return ResponseEntity.ok(ApiResponse.success(queryHistoryService.getQueriesByType(queryType)));
    }

    /**
     * 根据标签查询
     */
    @GetMapping("/tag/{tag}")
    public ResponseEntity<?> getQueriesByTag(@PathVariable String tag) {
        return ResponseEntity.ok(ApiResponse.success(queryHistoryService.getQueriesByTag(tag)));
    }
}
