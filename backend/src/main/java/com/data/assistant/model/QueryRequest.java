package com.data.assistant.model;

public class QueryRequest {
    private String naturalLanguageQuery;

    public QueryRequest() {}

    public QueryRequest(String naturalLanguageQuery) {
        this.naturalLanguageQuery = naturalLanguageQuery;
    }

    public String getNaturalLanguageQuery() {
        return naturalLanguageQuery;
    }

    public void setNaturalLanguageQuery(String naturalLanguageQuery) {
        this.naturalLanguageQuery = naturalLanguageQuery;
    }
}