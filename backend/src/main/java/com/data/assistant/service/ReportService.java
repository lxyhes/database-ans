package com.data.assistant.service;

import com.data.assistant.model.*;
import com.data.assistant.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportService {

    @Autowired
    private ReportTemplateRepository templateRepository;

    @Autowired
    private ReportInstanceRepository instanceRepository;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private AIService aiService;

    @Autowired
    private DataSource jdbcDataSource;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String REPORT_DIR = "reports/";

    @Transactional
    public ReportTemplate createTemplate(ReportTemplate template) {
        // 如果提供了自然语言查询，转换为SQL
        if (template.getNaturalLanguageQuery() != null && !template.getNaturalLanguageQuery().isEmpty()) {
            String sql = convertNaturalLanguageToSql(template.getDataSourceId(), template.getNaturalLanguageQuery());
            template.setGeneratedSql(sql);
        }
        return templateRepository.save(template);
    }

    @Transactional
    public ReportTemplate updateTemplate(Long id, ReportTemplate template) {
        ReportTemplate existing = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        existing.setName(template.getName());
        existing.setDescription(template.getDescription());
        existing.setNaturalLanguageQuery(template.getNaturalLanguageQuery());
        existing.setCronExpression(template.getCronExpression());
        existing.setIsActive(template.getIsActive());
        existing.setOutputFormat(template.getOutputFormat());

        // 如果自然语言查询变了，重新生成SQL
        if (template.getNaturalLanguageQuery() != null &&
                !template.getNaturalLanguageQuery().equals(existing.getNaturalLanguageQuery())) {
            String sql = convertNaturalLanguageToSql(existing.getDataSourceId(), template.getNaturalLanguageQuery());
            existing.setGeneratedSql(sql);
        }

        return templateRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<ReportTemplate> getTemplates(Long dataSourceId) {
        if (dataSourceId != null) {
            return templateRepository.findByDataSourceId(dataSourceId);
        }
        return templateRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ReportTemplate getTemplate(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
    }

    @Transactional
    public void deleteTemplate(Long id) {
        templateRepository.deleteById(id);
    }

    @Transactional
    public ReportInstance executeReport(Long templateId) {
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        ReportInstance instance = new ReportInstance();
        instance.setTemplateId(templateId);
        instance.setExecuteTime(LocalDateTime.now());
        instance.setStatus(ReportInstance.ExecuteStatus.RUNNING);
        instance = instanceRepository.save(instance);

        long startTime = System.currentTimeMillis();

        try {
            // 执行SQL查询
            List<Map<String, Object>> results = executeSql(template.getDataSourceId(), template.getGeneratedSql());

            // 生成文件
            String fileName = generateReportFile(template, results, instance.getId());

            // 更新实例
            instance.setStatus(ReportInstance.ExecuteStatus.SUCCESS);
            instance.setRowCount(results.size());
            instance.setResultData(objectMapper.writeValueAsString(results.subList(0, Math.min(results.size(), 100))));
            instance.setFileName(fileName);
            instance.setFileUrl("/api/reports/download/" + instance.getId());
            instance.setExecuteDuration(System.currentTimeMillis() - startTime);

            // 更新模板最后执行信息
            template.setLastExecuteTime(LocalDateTime.now());
            template.setLastExecuteStatus(ReportTemplate.ExecuteStatus.SUCCESS);
            templateRepository.save(template);

        } catch (Exception e) {
            instance.setStatus(ReportInstance.ExecuteStatus.FAILED);
            instance.setErrorMessage(e.getMessage());
            instance.setExecuteDuration(System.currentTimeMillis() - startTime);

            template.setLastExecuteTime(LocalDateTime.now());
            template.setLastExecuteStatus(ReportTemplate.ExecuteStatus.FAILED);
            templateRepository.save(template);
        }

        return instanceRepository.save(instance);
    }

    @Transactional(readOnly = true)
    public List<ReportInstance> getReportInstances(Long templateId) {
        return instanceRepository.findByTemplateIdOrderByExecuteTimeDesc(templateId);
    }

    @Transactional(readOnly = true)
    public ReportInstance getReportInstance(Long id) {
        return instanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report instance not found"));
    }

    @Scheduled(cron = "0 0 * * * *") // 每小时检查一次
    @Transactional
    public void scheduledExecuteReports() {
        List<ReportTemplate> activeTemplates = templateRepository.findByIsActiveTrue();

        for (ReportTemplate template : activeTemplates) {
            if (template.getCronExpression() != null && !template.getCronExpression().isEmpty()) {
                // 简化版：检查是否应该执行（实际应该使用CronExpression解析）
                if (shouldExecute(template)) {
                    executeReport(template.getId());
                }
            }
        }
    }

    private boolean shouldExecute(ReportTemplate template) {
        // 简化逻辑：如果上次执行超过1小时，且cron表达式不为空
        if (template.getLastExecuteTime() == null) {
            return true;
        }
        return template.getLastExecuteTime().plusHours(1).isBefore(LocalDateTime.now());
    }

    private String convertNaturalLanguageToSql(Long dataSourceId, String naturalLanguage) {
        // 使用AI服务转换自然语言为SQL
        NL2SQLRequest request = new NL2SQLRequest();
        request.setDataSourceId(dataSourceId);
        request.setQuestion(naturalLanguage);
        request.setProvider("mock");

        NL2SQLResult result = aiService.generateSQL(request);
        return result.getSql();
    }

    private List<Map<String, Object>> executeSql(Long dataSourceId, String sql) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection connection = jdbcDataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnLabel(i), rs.getObject(i));
                }
                results.add(row);
            }
        }

        return results;
    }

    private String generateReportFile(ReportTemplate template, List<Map<String, Object>> data, Long instanceId) throws Exception {
        // 创建报告目录
        File dir = new File(REPORT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName;

        switch (template.getOutputFormat()) {
            case CSV:
                fileName = String.format("%s_%s_%d.csv", template.getName(), timestamp, instanceId);
                generateCsvFile(data, REPORT_DIR + fileName);
                break;
            case PDF:
                fileName = String.format("%s_%s_%d.pdf", template.getName(), timestamp, instanceId);
                // PDF生成需要额外库，这里简化处理
                generateCsvFile(data, REPORT_DIR + fileName.replace(".pdf", ".csv"));
                break;
            case EXCEL:
            default:
                fileName = String.format("%s_%s_%d.csv", template.getName(), timestamp, instanceId);
                generateCsvFile(data, REPORT_DIR + fileName);
                break;
        }

        return fileName;
    }

    private void generateCsvFile(List<Map<String, Object>> data, String filePath) throws Exception {
        if (data.isEmpty()) return;

        try (FileWriter writer = new FileWriter(filePath)) {
            // 写入表头
            Set<String> headers = data.get(0).keySet();
            writer.write(String.join(",", headers));
            writer.write("\n");

            // 写入数据
            for (Map<String, Object> row : data) {
                List<String> values = new ArrayList<>();
                for (String header : headers) {
                    Object value = row.get(header);
                    values.add(value != null ? value.toString().replace(",", ";") : "");
                }
                writer.write(String.join(",", values));
                writer.write("\n");
            }
        }
    }

    public File getReportFile(Long instanceId) {
        ReportInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Report instance not found"));

        return new File(REPORT_DIR + instance.getFileName());
    }
}
