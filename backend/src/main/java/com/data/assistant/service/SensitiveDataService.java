package com.data.assistant.service;

import com.data.assistant.model.SensitiveColumn;
import com.data.assistant.model.SensitiveDataType;
import com.data.assistant.repository.SensitiveColumnRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class SensitiveDataService {

    @Autowired
    private SensitiveColumnRepository sensitiveColumnRepository;

    @Autowired
    private DataSource jdbcDataSource;

    // 字段名模式匹配
    private static final Map<SensitiveDataType, List<String>> COLUMN_NAME_PATTERNS = new HashMap<>();
    static {
        COLUMN_NAME_PATTERNS.put(SensitiveDataType.PHONE, Arrays.asList("phone", "mobile", "tel", "cell"));
        COLUMN_NAME_PATTERNS.put(SensitiveDataType.ID_CARD, Arrays.asList("id_card", "idcard", "identity", "sfz"));
        COLUMN_NAME_PATTERNS.put(SensitiveDataType.BANK_CARD, Arrays.asList("bank", "card_no", "account_no", "card_number"));
        COLUMN_NAME_PATTERNS.put(SensitiveDataType.EMAIL, Arrays.asList("email", "mail", "e_mail"));
        COLUMN_NAME_PATTERNS.put(SensitiveDataType.NAME, Arrays.asList("name", "username", "real_name", "full_name"));
        COLUMN_NAME_PATTERNS.put(SensitiveDataType.ADDRESS, Arrays.asList("address", "addr", "location"));
        COLUMN_NAME_PATTERNS.put(SensitiveDataType.PASSWORD, Arrays.asList("password", "pwd", "passwd", "secret"));
    }

    @Transactional
    public void scanTable(Long dataSourceId, String tableName) {
        // 清除该表的历史扫描结果
        sensitiveColumnRepository.deleteByDataSourceIdAndTableName(dataSourceId, tableName);

        try (Connection connection = jdbcDataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // 1. 基于字段名识别
            scanByColumnName(metaData, dataSourceId, tableName);

            // 2. 基于内容采样识别
            scanByContent(connection, dataSourceId, tableName);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to scan sensitive data", e);
        }
    }

    private void scanByColumnName(DatabaseMetaData metaData, Long dataSourceId, String tableName) throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, null, tableName, "%")) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String lowerColumnName = columnName.toLowerCase();

                for (Map.Entry<SensitiveDataType, List<String>> entry : COLUMN_NAME_PATTERNS.entrySet()) {
                    for (String pattern : entry.getValue()) {
                        if (lowerColumnName.contains(pattern)) {
                            saveSensitiveColumn(dataSourceId, tableName, columnName, entry.getKey(), "COLUMN_NAME", 0.8);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void scanByContent(Connection connection, Long dataSourceId, String tableName) throws SQLException {
        // 获取表的所有列
        List<String> columns = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, null, tableName, "%")) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }

        // 对每个列采样检查
        for (String column : columns) {
            // 跳过已识别的列
            Optional<SensitiveColumn> existing = sensitiveColumnRepository
                    .findByDataSourceIdAndTableNameAndColumnName(dataSourceId, tableName, column);
            if (existing.isPresent()) continue;

            // 采样数据
            String sql = String.format("SELECT DISTINCT %s FROM %s WHERE %s IS NOT NULL LIMIT 100", column, tableName, column);
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                Map<SensitiveDataType, Integer> matchCounts = new HashMap<>();
                int totalSamples = 0;

                while (rs.next()) {
                    String value = rs.getString(1);
                    if (value == null || value.isEmpty()) continue;
                    totalSamples++;

                    // 检查各种敏感数据类型
                    for (SensitiveDataType type : SensitiveDataType.values()) {
                        if (type.getPattern() != null && Pattern.matches(type.getPattern(), value)) {
                            matchCounts.merge(type, 1, Integer::sum);
                        }
                    }
                }

                // 如果匹配率超过阈值，标记为敏感列
                if (totalSamples > 0) {
                    for (Map.Entry<SensitiveDataType, Integer> entry : matchCounts.entrySet()) {
                        double matchRate = (double) entry.getValue() / totalSamples;
                        if (matchRate > 0.7) {
                            saveSensitiveColumn(dataSourceId, tableName, column, entry.getKey(), "CONTENT_SAMPLE", matchRate);
                        }
                    }
                }
            }
        }
    }

    private void saveSensitiveColumn(Long dataSourceId, String tableName, String columnName,
                                     SensitiveDataType type, String detectionMethod, double confidence) {
        SensitiveColumn sensitiveColumn = new SensitiveColumn();
        sensitiveColumn.setDataSourceId(dataSourceId);
        sensitiveColumn.setTableName(tableName);
        sensitiveColumn.setColumnName(columnName);
        sensitiveColumn.setSensitiveType(type);
        sensitiveColumn.setDetectionMethod(detectionMethod);
        sensitiveColumn.setConfidence(confidence);
        sensitiveColumn.setIsMasked(true);
        sensitiveColumn.setMaskRule(getDefaultMaskRule(type));

        sensitiveColumnRepository.save(sensitiveColumn);
    }

    private String getDefaultMaskRule(SensitiveDataType type) {
        switch (type) {
            case PHONE:
                return "MASK_MIDDLE_4";
            case ID_CARD:
                return "MASK_MIDDLE_8";
            case BANK_CARD:
                return "MASK_MIDDLE_8";
            case EMAIL:
                return "MASK_PREFIX";
            case NAME:
                return "MASK_PREFIX_1";
            case ADDRESS:
                return "MASK_SUFFIX";
            case PASSWORD:
                return "MASK_ALL";
            default:
                return "MASK_ALL";
        }
    }

    @Transactional(readOnly = true)
    public List<SensitiveColumn> getSensitiveColumns(Long dataSourceId) {
        return sensitiveColumnRepository.findByDataSourceId(dataSourceId);
    }

    @Transactional(readOnly = true)
    public List<SensitiveColumn> getSensitiveColumns(Long dataSourceId, String tableName) {
        return sensitiveColumnRepository.findByDataSourceIdAndTableName(dataSourceId, tableName);
    }

    @Transactional
    public void updateMaskConfig(Long columnId, Boolean isMasked, String maskRule) {
        SensitiveColumn column = sensitiveColumnRepository.findById(columnId)
                .orElseThrow(() -> new RuntimeException("Sensitive column not found"));
        column.setIsMasked(isMasked);
        if (maskRule != null) {
            column.setMaskRule(maskRule);
        }
        sensitiveColumnRepository.save(column);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSensitiveDataOverview(Long dataSourceId) {
        Map<String, Object> overview = new HashMap<>();

        List<SensitiveColumn> columns = sensitiveColumnRepository.findByDataSourceId(dataSourceId);

        // 按类型统计
        Map<String, Long> typeCount = new HashMap<>();
        Map<String, Long> tableCount = new HashMap<>();

        for (SensitiveColumn col : columns) {
            String typeName = col.getSensitiveType().getDisplayName();
            typeCount.merge(typeName, 1L, Long::sum);
            tableCount.merge(col.getTableName(), 1L, Long::sum);
        }

        overview.put("totalSensitiveColumns", columns.size());
        overview.put("typeDistribution", typeCount);
        overview.put("tableDistribution", tableCount);
        overview.put("maskedColumns", columns.stream().filter(SensitiveColumn::getIsMasked).count());

        return overview;
    }

    public String maskValue(String value, SensitiveDataType type, String maskRule) {
        if (value == null || value.isEmpty()) return value;

        switch (type) {
            case PHONE:
                return maskPhone(value);
            case ID_CARD:
                return maskIdCard(value);
            case BANK_CARD:
                return maskBankCard(value);
            case EMAIL:
                return maskEmail(value);
            case NAME:
                return maskName(value);
            case ADDRESS:
                return maskAddress(value);
            case PASSWORD:
                return "********";
            default:
                return value;
        }
    }

    private String maskPhone(String phone) {
        if (phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private String maskIdCard(String idCard) {
        if (idCard.length() != 18) return idCard;
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }

    private String maskBankCard(String card) {
        if (card.length() < 8) return card;
        return card.substring(0, 4) + " **** **** " + card.substring(card.length() - 4);
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return email;
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private String maskName(String name) {
        if (name.length() <= 1) return "*";
        return "*" + name.substring(1);
    }

    private String maskAddress(String address) {
        if (address.length() <= 6) return address;
        return address.substring(0, 6) + "****";
    }

    @Transactional
    public void deleteSensitiveColumn(Long columnId) {
        sensitiveColumnRepository.deleteById(columnId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> maskQueryResults(Long dataSourceId, String tableName, List<Map<String, Object>> results) {
        List<SensitiveColumn> sensitiveColumns = getSensitiveColumns(dataSourceId, tableName);

        if (sensitiveColumns.isEmpty() || results.isEmpty()) {
            return results;
        }

        // 创建敏感列映射
        Map<String, SensitiveColumn> sensitiveMap = new HashMap<>();
        for (SensitiveColumn col : sensitiveColumns) {
            if (col.getIsMasked()) {
                sensitiveMap.put(col.getColumnName().toLowerCase(), col);
            }
        }

        // 脱敏处理
        List<Map<String, Object>> maskedResults = new ArrayList<>();
        for (Map<String, Object> row : results) {
            Map<String, Object> maskedRow = new HashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String columnName = entry.getKey();
                Object value = entry.getValue();

                SensitiveColumn sensitiveCol = sensitiveMap.get(columnName.toLowerCase());
                if (sensitiveCol != null && value != null) {
                    maskedRow.put(columnName, maskValue(value.toString(), sensitiveCol.getSensitiveType(), sensitiveCol.getMaskRule()));
                } else {
                    maskedRow.put(columnName, value);
                }
            }
            maskedResults.add(maskedRow);
        }

        return maskedResults;
    }
}
