package com.data.assistant.model;

import java.util.List;
import java.util.Map;

public class QueryResponse {
    private boolean success;
    private String message;
    private List<Map<String, Object>> data;
    private String sqlQuery; // For debugging purposes

    public QueryResponse() {}

    public QueryResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public QueryResponse(boolean success, String message, List<Map<String, Object>> data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Map<String, Object>> getData() {
        return data;
    }

    public void setData(List<Map<String, Object>> data) {
        this.data = data;
    }

    public String getSqlQuery() {
        return sqlQuery;
    }

    public void setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }
}