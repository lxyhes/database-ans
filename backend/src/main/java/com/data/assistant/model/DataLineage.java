package com.data.assistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "data_lineage")
public class DataLineage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_source_id", nullable = false)
    private Long dataSourceId;

    @Column(name = "source_table", nullable = false)
    private String sourceTable;

    @Column(name = "source_column")
    private String sourceColumn;

    @Column(name = "target_table", nullable = false)
    private String targetTable;

    @Column(name = "target_column")
    private String targetColumn;

    @Column(name = "transformation_type")
    @Enumerated(EnumType.STRING)
    private TransformationType transformationType;

    @Column(name = "transformation_logic", columnDefinition = "TEXT")
    private String transformationLogic;

    @Column(name = "lineage_type")
    @Enumerated(EnumType.STRING)
    private LineageType lineageType;

    @Column(name = "sql_query", columnDefinition = "TEXT")
    private String sqlQuery;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum TransformationType {
        SELECT, JOIN, WHERE, GROUP_BY, ORDER_BY, AGGREGATION, CALCULATION, FILTER, UNION, SUBQUERY
    }

    public enum LineageType {
        QUERY, ETL, IMPORT, EXPORT, MANUAL
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDataSourceId() { return dataSourceId; }
    public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }

    public String getSourceTable() { return sourceTable; }
    public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }

    public String getSourceColumn() { return sourceColumn; }
    public void setSourceColumn(String sourceColumn) { this.sourceColumn = sourceColumn; }

    public String getTargetTable() { return targetTable; }
    public void setTargetTable(String targetTable) { this.targetTable = targetTable; }

    public String getTargetColumn() { return targetColumn; }
    public void setTargetColumn(String targetColumn) { this.targetColumn = targetColumn; }

    public TransformationType getTransformationType() { return transformationType; }
    public void setTransformationType(TransformationType transformationType) { this.transformationType = transformationType; }

    public String getTransformationLogic() { return transformationLogic; }
    public void setTransformationLogic(String transformationLogic) { this.transformationLogic = transformationLogic; }

    public LineageType getLineageType() { return lineageType; }
    public void setLineageType(LineageType lineageType) { this.lineageType = lineageType; }

    public String getSqlQuery() { return sqlQuery; }
    public void setSqlQuery(String sqlQuery) { this.sqlQuery = sqlQuery; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
