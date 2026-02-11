package com.data.assistant.model;

import java.util.*;

public class DataQualityReport {
    private String tableName;
    private Long totalRows;
    private Long totalColumns;
    private Double overallScore;
    private String grade;
    private Date checkTime;

    private List<MissingValueReport> missingValues;
    private List<DuplicateReport> duplicates;
    private List<OutlierReport> outliers;
    private List<FormatIssueReport> formatIssues;
    private List<ColumnQualityResult> columnQuality;
    private List<RepairSuggestion> repairSuggestions;

    public static class MissingValueReport {
        private String columnName;
        private Long missingCount;
        private Double missingPercentage;
        private String severity;

        public MissingValueReport(String columnName, Long missingCount, Double missingPercentage) {
            this.columnName = columnName;
            this.missingCount = missingCount;
            this.missingPercentage = missingPercentage;
            this.severity = missingPercentage > 50 ? "严重" : missingPercentage > 20 ? "警告" : "轻微";
        }

        // Getters and Setters
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public Long getMissingCount() { return missingCount; }
        public void setMissingCount(Long missingCount) { this.missingCount = missingCount; }
        public Double getMissingPercentage() { return missingPercentage; }
        public void setMissingPercentage(Double missingPercentage) { this.missingPercentage = missingPercentage; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
    }

    public static class DuplicateReport {
        private Long duplicateRowCount;
        private Double duplicatePercentage;
        private List<String> keyColumns;

        public DuplicateReport(Long duplicateRowCount, Double duplicatePercentage, List<String> keyColumns) {
            this.duplicateRowCount = duplicateRowCount;
            this.duplicatePercentage = duplicatePercentage;
            this.keyColumns = keyColumns;
        }

        // Getters and Setters
        public Long getDuplicateRowCount() { return duplicateRowCount; }
        public void setDuplicateRowCount(Long duplicateRowCount) { this.duplicateRowCount = duplicateRowCount; }
        public Double getDuplicatePercentage() { return duplicatePercentage; }
        public void setDuplicatePercentage(Double duplicatePercentage) { this.duplicatePercentage = duplicatePercentage; }
        public List<String> getKeyColumns() { return keyColumns; }
        public void setKeyColumns(List<String> keyColumns) { this.keyColumns = keyColumns; }
    }

    public static class OutlierReport {
        private String columnName;
        private Long outlierCount;
        private Double outlierPercentage;
        private String detectionMethod;
        private List<Object> sampleOutliers;
        private Double minNormal;
        private Double maxNormal;

        public OutlierReport(String columnName, Long outlierCount, Double outlierPercentage, String detectionMethod) {
            this.columnName = columnName;
            this.outlierCount = outlierCount;
            this.outlierPercentage = outlierPercentage;
            this.detectionMethod = detectionMethod;
            this.sampleOutliers = new ArrayList<>();
        }

        // Getters and Setters
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public Long getOutlierCount() { return outlierCount; }
        public void setOutlierCount(Long outlierCount) { this.outlierCount = outlierCount; }
        public Double getOutlierPercentage() { return outlierPercentage; }
        public void setOutlierPercentage(Double outlierPercentage) { this.outlierPercentage = outlierPercentage; }
        public String getDetectionMethod() { return detectionMethod; }
        public void setDetectionMethod(String detectionMethod) { this.detectionMethod = detectionMethod; }
        public List<Object> getSampleOutliers() { return sampleOutliers; }
        public void setSampleOutliers(List<Object> sampleOutliers) { this.sampleOutliers = sampleOutliers; }
        public Double getMinNormal() { return minNormal; }
        public void setMinNormal(Double minNormal) { this.minNormal = minNormal; }
        public Double getMaxNormal() { return maxNormal; }
        public void setMaxNormal(Double maxNormal) { this.maxNormal = maxNormal; }
    }

    public static class FormatIssueReport {
        private String columnName;
        private String expectedFormat;
        private Long issueCount;
        private List<String> sampleIssues;

        public FormatIssueReport(String columnName, String expectedFormat, Long issueCount) {
            this.columnName = columnName;
            this.expectedFormat = expectedFormat;
            this.issueCount = issueCount;
            this.sampleIssues = new ArrayList<>();
        }

        // Getters and Setters
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public String getExpectedFormat() { return expectedFormat; }
        public void setExpectedFormat(String expectedFormat) { this.expectedFormat = expectedFormat; }
        public Long getIssueCount() { return issueCount; }
        public void setIssueCount(Long issueCount) { this.issueCount = issueCount; }
        public List<String> getSampleIssues() { return sampleIssues; }
        public void setSampleIssues(List<String> sampleIssues) { this.sampleIssues = sampleIssues; }
    }

    public static class ColumnQualityResult {
        private String columnName;
        private String dataType;
        private Double completeness;
        private Double uniqueness;
        private Double validity;
        private String grade;

        public ColumnQualityResult(String columnName, String dataType) {
            this.columnName = columnName;
            this.dataType = dataType;
            this.completeness = 100.0;
            this.uniqueness = 100.0;
            this.validity = 100.0;
            this.grade = "A";
        }

        // Getters and Setters
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public Double getCompleteness() { return completeness; }
        public void setCompleteness(Double completeness) { this.completeness = completeness; }
        public Double getUniqueness() { return uniqueness; }
        public void setUniqueness(Double uniqueness) { this.uniqueness = uniqueness; }
        public Double getValidity() { return validity; }
        public void setValidity(Double validity) { this.validity = validity; }
        public String getGrade() { return grade; }
        public void setGrade(String grade) { this.grade = grade; }
    }

    public static class RepairSuggestion {
        private String issueType;
        private String description;
        private String suggestion;
        private Integer priority;

        public RepairSuggestion(String issueType, String description, String suggestion, Integer priority) {
            this.issueType = issueType;
            this.description = description;
            this.suggestion = suggestion;
            this.priority = priority;
        }

        // Getters and Setters
        public String getIssueType() { return issueType; }
        public void setIssueType(String issueType) { this.issueType = issueType; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
    }

    public DataQualityReport() {
        this.checkTime = new Date();
        this.missingValues = new ArrayList<>();
        this.duplicates = new ArrayList<>();
        this.outliers = new ArrayList<>();
        this.formatIssues = new ArrayList<>();
        this.columnQuality = new ArrayList<>();
        this.repairSuggestions = new ArrayList<>();
    }

    // Getters and Setters
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public Long getTotalRows() { return totalRows; }
    public void setTotalRows(Long totalRows) { this.totalRows = totalRows; }
    public Long getTotalColumns() { return totalColumns; }
    public void setTotalColumns(Long totalColumns) { this.totalColumns = totalColumns; }
    public Double getOverallScore() { return overallScore; }
    public void setOverallScore(Double overallScore) { this.overallScore = overallScore; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public Date getCheckTime() { return checkTime; }
    public void setCheckTime(Date checkTime) { this.checkTime = checkTime; }
    public List<MissingValueReport> getMissingValues() { return missingValues; }
    public void setMissingValues(List<MissingValueReport> missingValues) { this.missingValues = missingValues; }
    public List<DuplicateReport> getDuplicates() { return duplicates; }
    public void setDuplicates(List<DuplicateReport> duplicates) { this.duplicates = duplicates; }
    public List<OutlierReport> getOutliers() { return outliers; }
    public void setOutliers(List<OutlierReport> outliers) { this.outliers = outliers; }
    public List<FormatIssueReport> getFormatIssues() { return formatIssues; }
    public void setFormatIssues(List<FormatIssueReport> formatIssues) { this.formatIssues = formatIssues; }
    public List<ColumnQualityResult> getColumnQuality() { return columnQuality; }
    public void setColumnQuality(List<ColumnQualityResult> columnQuality) { this.columnQuality = columnQuality; }
    public List<RepairSuggestion> getRepairSuggestions() { return repairSuggestions; }
    public void setRepairSuggestions(List<RepairSuggestion> repairSuggestions) { this.repairSuggestions = repairSuggestions; }
}
