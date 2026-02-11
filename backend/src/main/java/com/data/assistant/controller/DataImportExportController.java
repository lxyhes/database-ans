package com.data.assistant.controller;

import com.data.assistant.service.DataImportExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 数据导入导出控制器
 */
@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = "*")
public class DataImportExportController {

    @Autowired
    private DataImportExportService dataImportExportService;

    /**
     * 导入 Excel
     */
    @PostMapping("/import/excel")
    public Map<String, Object> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tableName") String tableName) {
        try {
            return dataImportExportService.importExcel(file, tableName);
        } catch (Exception e) {
            return Map.of("success", false, "message", "导入失败: " + e.getMessage());
        }
    }

    /**
     * 导入 CSV
     */
    @PostMapping("/import/csv")
    public Map<String, Object> importCSV(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tableName") String tableName) {
        try {
            return dataImportExportService.importCSV(file, tableName);
        } catch (Exception e) {
            return Map.of("success", false, "message", "导入失败: " + e.getMessage());
        }
    }

    /**
     * 导出为 Excel
     */
    @PostMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(@RequestBody Map<String, String> request) {
        try {
            String sql = request.get("sql");
            String fileName = request.getOrDefault("fileName", "export");

            ByteArrayOutputStream outputStream = dataImportExportService.exportToExcel(sql, fileName);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String exportFileName = fileName + "_" + timestamp + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", exportFileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(outputStream.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 导出为 CSV
     */
    @PostMapping("/export/csv")
    public ResponseEntity<byte[]> exportCSV(@RequestBody Map<String, String> request) {
        try {
            String sql = request.get("sql");
            String fileName = request.getOrDefault("fileName", "export");

            ByteArrayOutputStream outputStream = dataImportExportService.exportToCSV(sql);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String exportFileName = fileName + "_" + timestamp + ".csv";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", exportFileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(outputStream.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}