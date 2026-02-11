package com.data.assistant.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 数据导入导出服务
 * 支持 Excel 和 CSV 格式
 */
@Service
public class DataImportExportService {
    private static final Logger logger = LoggerFactory.getLogger(DataImportExportService.class);

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DataImportExportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 导入 Excel 文件
     */
    public Map<String, Object> importExcel(MultipartFile file, String tableName) throws Exception {
        logger.info("Importing Excel file to table: {}", tableName);

        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        List<String> headers = new ArrayList<>();
        List<List<Object>> data = new ArrayList<>();

        // 读取表头
        Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell));
            }
        }

        // 读取数据
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            List<Object> rowData = new ArrayList<>();
            for (int j = 0; j < headers.size(); j++) {
                Cell cell = row.getCell(j);
                rowData.add(getCellValue(cell));
            }
            data.add(rowData);
        }

        // 创建表并插入数据
        createTableFromData(tableName, headers, data);
        workbook.close();

        return Map.of(
                "success", true,
                "tableName", tableName,
                "columnsImported", headers.size(),
                "rowsImported", data.size()
        );
    }

    /**
     * 导入 CSV 文件
     */
    public Map<String, Object> importCSV(MultipartFile file, String tableName) throws Exception {
        logger.info("Importing CSV file to table: {}", tableName);

        CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));

        String[] headers = reader.readNext();
        List<List<Object>> data = new ArrayList<>();

        String[] row;
        while ((row = reader.readNext()) != null) {
            List<Object> rowData = new ArrayList<>();
            for (String value : row) {
                rowData.add(value);
            }
            data.add(rowData);
        }
        reader.close();

        // 创建表并插入数据
        createTableFromData(tableName, Arrays.asList(headers), data);

        return Map.of(
                "success", true,
                "tableName", tableName,
                "columnsImported", headers.length,
                "rowsImported", data.size()
        );
    }

    /**
     * 导出为 Excel
     */
    public ByteArrayOutputStream exportToExcel(String sql, String fileName) throws Exception {
        logger.info("Exporting query results to Excel: {}", fileName);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Data");

        // 创建样式
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // 写入表头
        if (!results.isEmpty()) {
            Row headerRow = sheet.createRow(0);
            int colIndex = 0;
            for (String key : results.get(0).keySet()) {
                Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue(key);
                cell.setCellStyle(headerStyle);
            }
        }

        // 写入数据
        int rowIndex = 1;
        for (Map<String, Object> row : results) {
            Row dataRow = sheet.createRow(rowIndex++);
            int colIndex = 0;
            for (Object value : row.values()) {
                Cell cell = dataRow.createCell(colIndex++);
                setCellValue(cell, value);
                cell.setCellStyle(dataStyle);
            }
        }

        // 自动调整列宽
        for (int i = 0; i < results.get(0).size(); i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream;
    }

    /**
     * 导出为 CSV
     */
    public ByteArrayOutputStream exportToCSV(String sql) throws Exception {
        logger.info("Exporting query results to CSV");

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CSVWriter writer = new CSVWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

        if (!results.isEmpty()) {
            // 写入表头
            writer.writeNext(results.get(0).keySet().toArray(new String[0]));

            // 写入数据
            for (Map<String, Object> row : results) {
                String[] data = row.values().stream()
                        .map(v -> v != null ? v.toString() : "")
                        .toArray(String[]::new);
                writer.writeNext(data);
            }
        }

        writer.close();
        return outputStream;
    }

    /**
     * 根据数据创建表
     */
    private void createTableFromData(String tableName, List<String> headers, List<List<Object>> data) {
        // 删除旧表
        jdbcTemplate.execute(String.format("DROP TABLE IF EXISTS %s", tableName));

        // 创建新表
        StringBuilder createSql = new StringBuilder();
        createSql.append(String.format("CREATE TABLE %s (", tableName));
        for (int i = 0; i < headers.size(); i++) {
            if (i > 0) createSql.append(", ");
            createSql.append(String.format("%s VARCHAR(255)", headers.get(i)));
        }
        createSql.append(")");
        jdbcTemplate.execute(createSql.toString());

        // 插入数据
        if (!data.isEmpty()) {
            String insertSql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                    tableName,
                    String.join(", ", headers),
                    String.join(", ", Collections.nCopies(headers.size(), "?")));

            List<Object[]> batchArgs = new ArrayList<>();
            for (List<Object> row : data) {
                batchArgs.add(row.toArray(new Object[0]));
            }

            jdbcTemplate.batchUpdate(insertSql, batchArgs);
        }
    }

    /**
     * 获取单元格值
     */
    private Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> cell.getNumericCellValue();
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }

    /**
     * 获取单元格值（字符串）
     */
    private String getCellValueAsString(Cell cell) {
        Object value = getCellValue(cell);
        return value != null ? value.toString() : "";
    }

    /**
     * 设置单元格值
     */
    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }
}