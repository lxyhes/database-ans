package com.data.assistant.controller;

import com.data.assistant.model.ReportInstance;
import com.data.assistant.model.ReportTemplate;
import com.data.assistant.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping("/templates")
    public ResponseEntity<?> createTemplate(@RequestBody ReportTemplate template) {
        ReportTemplate created = reportService.createTemplate(template);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<?> updateTemplate(@PathVariable Long id, @RequestBody ReportTemplate template) {
        ReportTemplate updated = reportService.updateTemplate(id, template);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/templates")
    public ResponseEntity<?> getTemplates(@RequestParam(required = false) Long dataSourceId) {
        List<ReportTemplate> templates = reportService.getTemplates(dataSourceId);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/templates/{id}")
    public ResponseEntity<?> getTemplate(@PathVariable Long id) {
        ReportTemplate template = reportService.getTemplate(id);
        return ResponseEntity.ok(template);
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id) {
        reportService.deleteTemplate(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/templates/{id}/execute")
    public ResponseEntity<?> executeReport(@PathVariable Long id) {
        ReportInstance instance = reportService.executeReport(id);
        return ResponseEntity.ok(instance);
    }

    @GetMapping("/templates/{id}/instances")
    public ResponseEntity<?> getReportInstances(@PathVariable Long id) {
        List<ReportInstance> instances = reportService.getReportInstances(id);
        return ResponseEntity.ok(instances);
    }

    @GetMapping("/download/{instanceId}")
    public ResponseEntity<?> downloadReport(@PathVariable Long instanceId) {
        File file = reportService.getReportFile(instanceId);

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new FileSystemResource(file));
    }
}
