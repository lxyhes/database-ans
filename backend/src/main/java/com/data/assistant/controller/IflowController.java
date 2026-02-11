package com.data.assistant.controller;

import com.data.assistant.model.QueryRequest;
import com.data.assistant.model.QueryResponse;
import com.data.assistant.service.IflowClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * iFlow Client SDK 控制器
 * 提供基于 iFlow Java SDK 的智能数据分析 API
 */
@RestController
@RequestMapping("/api/iflow")
@CrossOrigin(origins = "*")
public class IflowController {

    @Autowired
    private IflowClientService iflowClientService;

    /**
     * 简单查询接口
     */
    @PostMapping("/query")
    public QueryResponse query(@RequestBody QueryRequest request) {
        String result = iflowClientService.simpleQuery(request.getNaturalLanguageQuery());
        return new QueryResponse(true, result);
    }

    /**
     * 异步查询接口
     */
    @PostMapping("/query-async")
    public CompletableFuture<QueryResponse> queryAsync(@RequestBody QueryRequest request) {
        return iflowClientService.asyncQuery(request.getNaturalLanguageQuery())
                .thenApply(result -> new QueryResponse(true, result));
    }

    /**
     * 数据分析接口
     */
    @PostMapping("/analyze")
    public QueryResponse analyze(@RequestBody DataAnalysisRequest request) {
        String result = iflowClientService.analyzeData(request.getDataContext(), request.getQuestion());
        return new QueryResponse(true, result);
    }

    /**
     * SQL 生成接口
     */
    @PostMapping("/generate-sql")
    public QueryResponse generateSql(@RequestBody SqlGenerationRequest request) {
        String result = iflowClientService.generateSql(request.getSchema(), request.getNaturalLanguageQuery());
        return new QueryResponse(true, result);
    }

    /**
     * 代码生成接口
     */
    @PostMapping("/generate-code")
    public QueryResponse generateCode(@RequestBody CodeGenerationRequest request) {
        String result = iflowClientService.generateCode(request.getDescription(), request.getLanguage());
        return new QueryResponse(true, result);
    }

    /**
     * 代码解释接口
     */
    @PostMapping("/explain-code")
    public QueryResponse explainCode(@RequestBody CodeExplanationRequest request) {
        String result = iflowClientService.explainCode(request.getCode());
        return new QueryResponse(true, result);
    }

    /**
     * 数据洞察接口
     */
    @PostMapping("/insights")
    public QueryResponse getInsights(@RequestBody DataInsightsRequest request) {
        String result = iflowClientService.getDataInsights(request.getDataSummary());
        return new QueryResponse(true, result);
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public QueryResponse health() {
        boolean available = iflowClientService.isIflowAvailable();
        String message = available ? "iFlow SDK is available" : "iFlow SDK is not available";
        return new QueryResponse(available, message);
    }

    /**
     * 数据分析请求体
     */
    public static class DataAnalysisRequest {
        private String dataContext;
        private String question;

        public String getDataContext() { return dataContext; }
        public void setDataContext(String dataContext) { this.dataContext = dataContext; }
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
    }

    /**
     * SQL 生成请求体
     */
    public static class SqlGenerationRequest {
        private String schema;
        private String naturalLanguageQuery;

        public String getSchema() { return schema; }
        public void setSchema(String schema) { this.schema = schema; }
        public String getNaturalLanguageQuery() { return naturalLanguageQuery; }
        public void setNaturalLanguageQuery(String naturalLanguageQuery) { this.naturalLanguageQuery = naturalLanguageQuery; }
    }

    /**
     * 代码生成请求体
     */
    public static class CodeGenerationRequest {
        private String description;
        private String language;

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }

    /**
     * 代码解释请求体
     */
    public static class CodeExplanationRequest {
        private String code;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    /**
     * 数据洞察请求体
     */
    public static class DataInsightsRequest {
        private String dataSummary;

        public String getDataSummary() { return dataSummary; }
        public void setDataSummary(String dataSummary) { this.dataSummary = dataSummary; }
    }
}