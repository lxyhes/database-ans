package com.data.assistant.controller;

import com.data.assistant.model.QueryRequest;
import com.data.assistant.model.QueryResponse;
import com.data.assistant.service.DataQueryService;
import com.data.assistant.service.NaturalLanguageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/query")
@CrossOrigin(origins = "*")
public class QueryController {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);
    
    @Autowired
    private NaturalLanguageProcessor naturalLanguageProcessor;
    
    @Autowired
    private DataQueryService dataQueryService;
    
    @PostMapping("/natural")
    public QueryResponse processNaturalLanguageQuery(@RequestBody QueryRequest request) {
        try {
            if (request.getDataSourceId() != null) {
                dataQueryService.switchDataSource(request.getDataSourceId());
            }
            
            String sqlQuery = naturalLanguageProcessor.parseNaturalLanguageToSQL(request.getNaturalLanguageQuery());
            
            List<Map<String, Object>> results = dataQueryService.executeQuery(sqlQuery);
            
            QueryResponse response = new QueryResponse(true, "Query processed successfully");
            response.setData(results);
            response.setSqlQuery(sqlQuery);
            
            return response;
        } catch (Exception e) {
            logger.error("Error processing query", e);
            return new QueryResponse(false, "Error processing query: " + e.getMessage());
        }
    }
    
    @PostMapping("/analyze")
    public QueryResponse processAnalysisQuery(@RequestBody QueryRequest request) {
        try {
            if (request.getDataSourceId() != null) {
                dataQueryService.switchDataSource(request.getDataSourceId());
            }
            
            String analysisType = extractAnalysisType(request.getNaturalLanguageQuery());
            
            String sqlQuery = naturalLanguageProcessor.parseNaturalLanguageToSQL(request.getNaturalLanguageQuery());
            
            List<Map<String, Object>> results = dataQueryService.executeAnalysisQuery(analysisType, sqlQuery);
            
            QueryResponse response = new QueryResponse(true, "Analysis completed successfully");
            response.setData(results);
            response.setSqlQuery(sqlQuery);
            
            return response;
        } catch (Exception e) {
            logger.error("Error processing analysis", e);
            return new QueryResponse(false, "Error processing analysis: " + e.getMessage());
        }
    }
    
    private String extractAnalysisType(String query) {
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("summary") || lowerQuery.contains("summarize") || 
            lowerQuery.contains("overview") || lowerQuery.contains("total") ||
            lowerQuery.contains("总") || lowerQuery.contains("统计") || lowerQuery.contains("汇总")) {
            return "summary";
        } else if (lowerQuery.contains("trend") || lowerQuery.contains("change") || 
                   lowerQuery.contains("increase") || lowerQuery.contains("decrease") ||
                   lowerQuery.contains("趋势") || lowerQuery.contains("变化")) {
            return "trend";
        } else if (lowerQuery.contains("compare") || lowerQuery.contains("comparison") || 
                   lowerQuery.contains("vs") || lowerQuery.contains("versus") ||
                   lowerQuery.contains("对比") || lowerQuery.contains("比较")) {
            return "comparison";
        } else if (lowerQuery.contains("predict") || lowerQuery.contains("forecast") || 
                   lowerQuery.contains("estimate") || lowerQuery.contains("projection") ||
                   lowerQuery.contains("预测") || lowerQuery.contains("预估")) {
            return "forecast";
        } else {
            return "summary";
        }
    }
    
    @GetMapping("/health")
    public String healthCheck() {
        return "Data Analysis Assistant Backend is running!";
    }
}
