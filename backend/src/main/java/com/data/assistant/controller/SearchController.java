package com.data.assistant.controller;

import com.data.assistant.common.ApiResponse;
import com.data.assistant.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
public class SearchController {
    
    @Autowired
    private SearchService searchService;
    
    @GetMapping("/global")
    public ResponseEntity<Map<String, Object>> globalSearch(
            @RequestParam String keyword,
            @RequestParam(required = false) String types) {
        
        List<String> typeList = types != null && !types.isEmpty() ? 
            Arrays.asList(types.split(",")) : null;
        
        return ResponseEntity.ok(ApiResponse.success(
            searchService.globalSearch(keyword, typeList)
        ));
    }
    
    @GetMapping("/suggestions")
    public ResponseEntity<Map<String, Object>> getSuggestions(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(
            searchService.getSearchSuggestions(keyword)
        ));
    }
}
