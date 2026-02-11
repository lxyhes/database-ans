package com.data.assistant.service;

import com.data.assistant.model.DataQualityReport;
import com.data.assistant.model.DataSource;
import com.data.assistant.repository.DataSourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource as JdbcDataSource;
import java.sql.*;
import java.util.*;

@Service
public class DataQualityService {

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private javax.sql.DataSource jdbcDataSource;

    public DataQualityReport checkTableQuality(Long dataSourceId, String tableName) {
        DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new RuntimeException("DataSource not found"));

        DataQualityReport report = new DataQualityReport();
        report.setTableName(tableName);

        try (Connection connection = jdbcDataSource.getConnection()) {
            // 获取表基本信息
            getTableBasicInfo(connection, tableName, report);

            // 检查缺失值
            checkMissingValues(connection, tableName, report);

            // 检查重复数据
            checkDuplicates(connection, tableName, report);

            // 检查异常值
            checkOutliers(connection, tableName, report);

            // 检查格式问题
            checkFormatIssues(connection, tableName, report);

            // 计算综合评分
            calculateOverallScore(report);

            // 生成修复建议
            generateRepairSuggestions(report);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check data quality", e);
        }

        return report;
    }

    private void getTableBasicInfo(Connection connection, String tableName, DataQualityReport report) throws SQLException {
        String countSql = String.format("SELECT COUNT(*) FROM %s", tableName);
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next()) {
                report.setTotalRows(rs.getLong(1));
            }
        }

        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, null, tableName, "%")) {
            long columnCount = 0;
            while (rs.next()) {
                columnCount++;
                String columnName = rs.getString("COLUMN_NAME");
                String dataType = rs.getString("TYPE_NAME");
                report.getColumnQuality().add(new DataQualityReport.ColumnQualityResult(columnName, dataType));
            }
            report.setTotalColumns(columnCount);
        }
    }

    private void checkMissingValues(Connection connection, String tableName, DataQualityReport report) throws SQLException {
        long totalRows = report.getTotalRows();

        for (DataQualityReport.ColumnQualityResult column : report.getColumnQuality()) {
            String columnName = column.getColumnName();
            String sql = String.format(
                "SELECT COUNT(*) FROM %s WHERE %s IS NULL OR %s = ''",
                tableName, columnName, columnName
            );

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    long missingCount = rs.getLong(1);
                    double missingPercentage = totalRows > 0 ? (missingCount * 100.0 / totalRows) : 0;

                    if (missingCount > 0) {
                        report.getMissingValues().add(
                            new DataQualityReport.MissingValueReport(columnName, missingCount, missingPercentage)
                        );
                    }

                    column.setCompleteness(100.0 - missingPercentage);
                }
            }
        }
    }

    private void checkDuplicates(Connection connection, String tableName, DataQualityReport report) throws SQLException {
        // 获取所有列
        List<String> columns = new ArrayList<>();
        for (DataQualityReport.ColumnQualityResult col : report.getColumnQuality()) {
            columns.add(col.getColumnName());
        }

        if (columns.isEmpty()) return;

        String columnList = String.join(", ", columns);
        String sql = String.format(
            "SELECT %s, COUNT(*) as cnt FROM %s GROUP BY %s HAVING cnt > 1",
            columnList, tableName, columnList
        );

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            long duplicateCount = 0;
            while (rs.next()) {
                duplicateCount++;
            }

            double duplicatePercentage = report.getTotalRows() > 0 ?
                (duplicateCount * 100.0 / report.getTotalRows()) : 0;

            if (duplicateCount > 0) {
                report.getDuplicates().add(
                    new DataQualityReport.DuplicateReport(duplicateCount, duplicatePercentage, columns)
                );
            }
        }
    }

    private void checkOutliers(Connection connection, String tableName, DataQualityReport report) throws SQLException {
        for (DataQualityReport.ColumnQualityResult column : report.getColumnQuality()) {
            String columnName = column.getColumnName();
            String dataType = column.getDataType().toUpperCase();

            // 只检查数值类型
            if (!isNumericType(dataType)) continue;

            // 使用 IQR 方法检测异常值
            String sql = String.format(
                "SELECT %s FROM %s WHERE %s IS NOT NULL",
                columnName, tableName, columnName
            );

            List<Double> values = new ArrayList<>();
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    double val = rs.getDouble(1);
                    if (!rs.wasNull()) {
                        values.add(val);
                    }
                }
            }

            if (values.size() < 4) continue;

            // 计算四分位数
            Collections.sort(values);
            int n = values.size();
            double q1 = values.get(n / 4);
            double q3 = values.get(3 * n / 4);
            double iqr = q3 - q1;
            double lowerBound = q1 - 1.5 * iqr;
            double upperBound = q3 + 1.5 * iqr;

            // 统计异常值
            long outlierCount = 0;
            List<Object> sampleOutliers = new ArrayList<>();
            for (Double val : values) {
                if (val < lowerBound || val > upperBound) {
                    outlierCount++;
                    if (sampleOutliers.size() < 5) {
                        sampleOutliers.add(val);
                    }
                }
            }

            if (outlierCount > 0) {
                double outlierPercentage = outlierCount * 100.0 / values.size();
                DataQualityReport.OutlierReport outlierReport = new DataQualityReport.OutlierReport(
                    columnName, outlierCount, outlierPercentage, "IQR"
                );
                outlierReport.setMinNormal(lowerBound);
                outlierReport.setMaxNormal(upperBound);
                outlierReport.setSampleOutliers(sampleOutliers);
                report.getOutliers().add(outlierReport);
            }
        }
    }

    private void checkFormatIssues(Connection connection, String tableName, DataQualityReport report) throws SQLException {
        for (DataQualityReport.ColumnQualityResult column : report.getColumnQuality()) {
            String columnName = column.getColumnName();

            // 检查手机号格式
            checkPhoneFormat(connection, tableName, columnName, report);

            // 检查邮箱格式
            checkEmailFormat(connection, tableName, columnName, report);

            // 检查日期格式
            checkDateFormat(connection, tableName, columnName, column.getDataType(), report);
        }
    }

    private void checkPhoneFormat(Connection connection, String tableName, String columnName, DataQualityReport report) throws SQLException {
        if (!columnName.toLowerCase().contains("phone") &&
            !columnName.toLowerCase().contains("mobile") &&
            !columnName.toLowerCase().contains("tel")) {
            return;
        }

        String sql = String.format(
            "SELECT COUNT(*) FROM %s WHERE %s IS NOT NULL AND %s NOT REGEXP '^1[3-9][0-9]{9}$'",
            tableName, columnName, columnName
        );

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next() && rs.getLong(1) > 0) {
                report.getFormatIssues().add(
                    new DataQualityReport.FormatIssueReport(columnName, "手机号格式", rs.getLong(1))
                );
            }
        } catch (SQLException e) {
            // 某些数据库不支持 REGEXP，忽略错误
        }
    }

    private void checkEmailFormat(Connection connection, String tableName, String columnName, DataQualityReport report) throws SQLException {
        if (!columnName.toLowerCase().contains("email") &&
            !columnName.toLowerCase().contains("mail")) {
            return;
        }

        String sql = String.format(
            "SELECT COUNT(*) FROM %s WHERE %s IS NOT NULL AND %s NOT LIKE '%%@%%.%%'",
            tableName, columnName, columnName
        );

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next() && rs.getLong(1) > 0) {
                report.getFormatIssues().add(
                    new DataQualityReport.FormatIssueReport(columnName, "邮箱格式", rs.getLong(1))
                );
            }
        }
    }

    private void checkDateFormat(Connection connection, String tableName, String columnName, String dataType, DataQualityReport report) throws SQLException {
        if (!dataType.toUpperCase().contains("DATE") &&
            !dataType.toUpperCase().contains("TIME")) {
            return;
        }

        String sql = String.format(
            "SELECT COUNT(*) FROM %s WHERE %s IS NOT NULL AND %s > NOW() + INTERVAL 100 YEAR",
            tableName, columnName, columnName
        );

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next() && rs.getLong(1) > 0) {
                report.getFormatIssues().add(
                    new DataQualityReport.FormatIssueReport(columnName, "合理日期范围", rs.getLong(1))
                );
            }
        } catch (SQLException e) {
            // 语法可能因数据库而异，忽略错误
        }
    }

    private void calculateOverallScore(DataQualityReport report) {
        double totalScore = 0;
        int columnCount = report.getColumnQuality().size();

        for (DataQualityReport.ColumnQualityResult column : report.getColumnQuality()) {
            double columnScore = (column.getCompleteness() + column.getUniqueness() + column.getValidity()) / 3;
            totalScore += columnScore;

            // 设置列等级
            if (columnScore >= 95) {
                column.setGrade("A");
            } else if (columnScore >= 80) {
                column.setGrade("B");
            } else if (columnScore >= 60) {
                column.setGrade("C");
            } else {
                column.setGrade("D");
            }
        }

        double overallScore = columnCount > 0 ? totalScore / columnCount : 100;
        report.setOverallScore(overallScore);

        if (overallScore >= 95) {
            report.setGrade("A");
        } else if (overallScore >= 80) {
            report.setGrade("B");
        } else if (overallScore >= 60) {
            report.setGrade("C");
        } else {
            report.setGrade("D");
        }
    }

    private void generateRepairSuggestions(DataQualityReport report) {
        // 缺失值建议
        for (DataQualityReport.MissingValueReport missing : report.getMissingValues()) {
            if (missing.getMissingPercentage() > 50) {
                report.getRepairSuggestions().add(new DataQualityReport.RepairSuggestion(
                    "缺失值",
                    String.format("列 '%s' 缺失率高达 %.1f%%", missing.getColumnName(), missing.getMissingPercentage()),
                    "建议：检查数据采集流程，或考虑删除该列",
                    1
                ));
            } else if (missing.getMissingPercentage() > 20) {
                report.getRepairSuggestions().add(new DataQualityReport.RepairSuggestion(
                    "缺失值",
                    String.format("列 '%s' 缺失率为 %.1f%%", missing.getColumnName(), missing.getMissingPercentage()),
                    "建议：使用均值/中位数填充，或标记为未知",
                    2
                ));
            }
        }

        // 重复数据建议
        for (DataQualityReport.DuplicateReport duplicate : report.getDuplicates()) {
            report.getRepairSuggestions().add(new DataQualityReport.RepairSuggestion(
                "重复数据",
                String.format("发现 %d 条重复记录 (%.1f%%)", duplicate.getDuplicateRowCount(), duplicate.getDuplicatePercentage()),
                "建议：添加唯一约束或清理重复数据",
                1
            ));
        }

        // 异常值建议
        for (DataQualityReport.OutlierReport outlier : report.getOutliers()) {
            report.getRepairSuggestions().add(new DataQualityReport.RepairSuggestion(
                "异常值",
                String.format("列 '%s' 有 %d 个异常值", outlier.getColumnName(), outlier.getOutlierCount()),
                "建议：核实数据准确性，或使用截断/标准化处理",
                3
            ));
        }

        // 格式问题建议
        for (DataQualityReport.FormatIssueReport issue : report.getFormatIssues()) {
            report.getRepairSuggestions().add(new DataQualityReport.RepairSuggestion(
                "格式问题",
                String.format("列 '%s' 有 %d 条格式不符记录", issue.getColumnName(), issue.getIssueCount()),
                "建议：添加数据校验规则，清洗不符合格式的数据",
                2
            ));
        }

        // 按优先级排序
        report.getRepairSuggestions().sort(Comparator.comparingInt(DataQualityReport.RepairSuggestion::getPriority));
    }

    private boolean isNumericType(String dataType) {
        return dataType.contains("INT") ||
               dataType.contains("DECIMAL") ||
               dataType.contains("NUMERIC") ||
               dataType.contains("FLOAT") ||
               dataType.contains("DOUBLE") ||
               dataType.contains("REAL") ||
               dataType.contains("NUMBER");
    }
}
