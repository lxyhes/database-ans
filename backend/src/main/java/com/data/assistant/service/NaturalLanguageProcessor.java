package com.data.assistant.service;

import org.springframework.stereotype.Service;

@Service
public class NaturalLanguageProcessor {
    public String parseNaturalLanguageToSQL(String query) {
        return "SELECT * FROM data LIMIT 10";
    }
}
