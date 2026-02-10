package com.data.assistant.controller;

import com.data.assistant.model.QueryRequest;
import com.data.assistant.model.QueryResponse;
import com.data.assistant.service.DataQueryService;
import com.data.assistant.service.NaturalLanguageProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/query")
@CrossOrigin(origins = "*") // 允许前端跨域访问
public class QueryController {
    
    @Autowired
    private NaturalLanguageProcessor naturalLanguageProcessor;
    
    @Autowired
    private DataQueryService dataQueryService;
    
    @PostMapping("/natural")
    public QueryResponse processNaturalLanguageQuery(@RequestBody QueryRequest request) {
        try {
            // 将自然语言查询转换为SQL
            String sqlQuery = naturalLanguageProcessor.parseNaturalLanguageToSQL(request.getNaturalLanguageQuery());
            
            // 执行SQL查询
            List<Map<String, Object>> results = dataQueryService.executeQuery(sqlQuery);
            
            // 返回成功响应
            QueryResponse response = new QueryResponse(true, "Query processed successfully");
            response.setData(results);
            response.setSqlQuery(sqlQuery); // 用于调试
            
            return response;
        } catch (Exception e) {
            // 返回错误响应
            return new QueryResponse(false, "Error processing query: " + e.getMessage());
        }
    }
    
    /**
     * 执行数据分析
     */
    @PostMapping("/analyze")
    public QueryResponse processAnalysisQuery(@RequestBody QueryRequest request) {
        try {
            // 从自然语言中提取分析类型
            String analysisType = extractAnalysisType(request.getNaturalLanguageQuery());
            
            // 将自然语言查询转换为SQL
            String sqlQuery = naturalLanguageProcessor.parseNaturalLanguageToSQL(request.getNaturalLanguageQuery());
            
            // 执行数据分析
            List<Map<String, Object>> results = dataQueryService.executeAnalysisQuery(analysisType, sqlQuery);
            
            // 返回成功响应
            QueryResponse response = new QueryResponse(true, "Analysis completed successfully");
            response.setData(results);
            response.setSqlQuery(sqlQuery); // 用于调试
            
            return response;
        } catch (Exception e) {
            // 返回错误响应
            return new QueryResponse(false, "Error processing analysis: " + e.getMessage());
        }
    }
    
    /**
     * 从自然语言查询中提取分析类型
     */
    private String extractAnalysisType(String query) {
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("summary") || lowerQuery.contains("summarize") || 
            lowerQuery.contains("overview") || lowerQuery.contains("total")) {
            return "summary";
        } else if (lowerQuery.contains("trend") || lowerQuery.contains("change") || 
                   lowerQuery.contains("increase") || lowerQuery.contains("decrease")) {
            return "trend";
        } else if (lowerQuery.contains("compare") || lowerQuery.contains("comparison") || 
                   lowerQuery.contains("vs") || lowerQuery.contains("versus")) {
            return "comparison";
        } else if (lowerQuery.contains("predict") || lowerQuery.contains("forecast") || 
                   lowerQuery.contains("estimate") || lowerQuery.contains("projection")) {
            return "forecast";
        } else {
            // 默认返回摘要统计
            return "summary";
        }
    }
    
    // 提供一个简单的健康检查端点
    @GetMapping("/health")
    public String healthCheck() {
        return "Data Analysis Assistant Backend is running!";
    }
}