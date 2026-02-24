package com.data.assistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_instance")
public class ReportInstance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "data_source_id")
    private Long dataSourceId;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(name = "table_names", length = 500)
    private String tableNames;
    
    @Column(name = "report_data", columnDefinition = "TEXT")
    private String reportData;
    
    @Column(name = "health_score")
    private Integer healthScore;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getDataSourceId() { return dataSourceId; }
    public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getTableNames() { return tableNames; }
    public void setTableNames(String tableNames) { this.tableNames = tableNames; }
    
    public String getReportData() { return reportData; }
    public void setReportData(String reportData) { this.reportData = reportData; }
    
    public Integer getHealthScore() { return healthScore; }
    public void setHealthScore(Integer healthScore) { this.healthScore = healthScore; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
