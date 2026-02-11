package com.data.assistant.controller;

import com.data.assistant.model.QueryHistory;
import com.data.assistant.service.QueryHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public List<QueryHistory> getAllQueries() {
        return queryHistoryService.getAllQueries();
    }

    /**
     * 获取收藏的查询
     */
    @GetMapping("/favorites")
    public List<QueryHistory> getFavoriteQueries() {
        return queryHistoryService.getFavoriteQueries();
    }

    /**
     * 保存查询
     */
    @PostMapping("/save")
    public QueryHistory saveQuery(@RequestBody Map<String, String> request) {
        String queryText = request.get("queryText");
        String sqlQuery = request.get("sqlQuery");
        String queryType = request.getOrDefault("queryType", "general");
        String tags = request.get("tags");
        return queryHistoryService.saveQuery(queryText, sqlQuery, queryType, tags);
    }

    /**
     * 切换收藏状态
     */
    @PostMapping("/{id}/toggle-favorite")
    public QueryHistory toggleFavorite(@PathVariable Long id) {
        return queryHistoryService.toggleFavorite(id);
    }

    /**
     * 删除查询
     */
    @DeleteMapping("/{id}")
    public Map<String, String> deleteQuery(@PathVariable Long id) {
        queryHistoryService.deleteQuery(id);
        return Map.of("message", "删除成功");
    }

    /**
     * 根据类型查询
     */
    @GetMapping("/type/{queryType}")
    public List<QueryHistory> getQueriesByType(@PathVariable String queryType) {
        return queryHistoryService.getQueriesByType(queryType);
    }

    /**
     * 根据标签查询
     */
    @GetMapping("/tag/{tag}")
    public List<QueryHistory> getQueriesByTag(@PathVariable String tag) {
        return queryHistoryService.getQueriesByTag(tag);
    }
}