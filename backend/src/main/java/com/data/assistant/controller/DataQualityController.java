package com.data.assistant.controller;

import com.data.assistant.model.DataQualityReport;
import com.data.assistant.service.DataQualityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/data-quality")
@CrossOrigin(origins = "*")
public class DataQualityController {

    @Autowired
    private DataQualityService dataQualityService;

    @GetMapping("/check/{dataSourceId}")
    public ResponseEntity<?> checkTableQuality(
            @PathVariable Long dataSourceId,
            @RequestParam String tableName) {
        DataQualityReport report = dataQualityService.checkTableQuality(dataSourceId, tableName);
        return ResponseEntity.ok(report);
    }
}
