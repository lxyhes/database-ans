package com.data.assistant.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DataQueryService {
    public List<Map<String, Object>> executeQuery(String sql) {
        return new ArrayList<>();
    }
    
    public List<Map<String, Object>> executeAnalysisQuery(String type, String sql) {
        return new ArrayList<>();
    }
}
