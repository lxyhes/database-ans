package com.data.assistant.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_rule")
public class AlertRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "data_source_id", nullable = false)
    private Long dataSourceId;
    
    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;
    
    @Column(name = "column_name", length = 100)
    private String columnName;
    
    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType;
    
    @Column(length = 20)
    private String operator;
    
    @Column(name = "threshold_value", precision = 20, scale = 4)
    private BigDecimal thresholdValue;
    
    @Column(name = "threshold_value2", precision = 20, scale = 4)
    private BigDecimal thresholdValue2;
    
    @Column(name = "detection_method", length = 50)
    private String detectionMethod;
    
    @Column(precision = 5, scale = 2)
    private BigDecimal sensitivity = new BigDecimal("3.00");
    
    @Column(name = "baseline_period")
    private Integer baselinePeriod = 7;
    
    @Column(name = "check_interval")
    private Integer checkInterval = 5;
    
    @Column(name = "check_sql", columnDefinition = "TEXT")
    private String checkSql;
    
    @Column(name = "alert_level", length = 20)
    private String alertLevel = "WARNING";
    
    @Column(name = "alert_channels", length = 500)
    private String alertChannels;
    
    @Column(name = "alert_receivers", length = 1000)
    private String alertReceivers;
    
    @Column(name = "cooldown_minutes")
    private Integer cooldownMinutes = 30;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "last_check_time")
    private LocalDateTime lastCheckTime;
    
    @Column(name = "last_alert_time")
    private LocalDateTime lastAlertTime;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Long getDataSourceId() { return dataSourceId; }
    public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
    
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    
    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }
    
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    
    public BigDecimal getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(BigDecimal thresholdValue) { this.thresholdValue = thresholdValue; }
    
    public BigDecimal getThresholdValue2() { return thresholdValue2; }
    public void setThresholdValue2(BigDecimal thresholdValue2) { this.thresholdValue2 = thresholdValue2; }
    
    public String getDetectionMethod() { return detectionMethod; }
    public void setDetectionMethod(String detectionMethod) { this.detectionMethod = detectionMethod; }
    
    public BigDecimal getSensitivity() { return sensitivity; }
    public void setSensitivity(BigDecimal sensitivity) { this.sensitivity = sensitivity; }
    
    public Integer getBaselinePeriod() { return baselinePeriod; }
    public void setBaselinePeriod(Integer baselinePeriod) { this.baselinePeriod = baselinePeriod; }
    
    public Integer getCheckInterval() { return checkInterval; }
    public void setCheckInterval(Integer checkInterval) { this.checkInterval = checkInterval; }
    
    public String getCheckSql() { return checkSql; }
    public void setCheckSql(String checkSql) { this.checkSql = checkSql; }
    
    public String getAlertLevel() { return alertLevel; }
    public void setAlertLevel(String alertLevel) { this.alertLevel = alertLevel; }
    
    public String getAlertChannels() { return alertChannels; }
    public void setAlertChannels(String alertChannels) { this.alertChannels = alertChannels; }
    
    public String getAlertReceivers() { return alertReceivers; }
    public void setAlertReceivers(String alertReceivers) { this.alertReceivers = alertReceivers; }
    
    public Integer getCooldownMinutes() { return cooldownMinutes; }
    public void setCooldownMinutes(Integer cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getLastCheckTime() { return lastCheckTime; }
    public void setLastCheckTime(LocalDateTime lastCheckTime) { this.lastCheckTime = lastCheckTime; }
    
    public LocalDateTime getLastAlertTime() { return lastAlertTime; }
    public void setLastAlertTime(LocalDateTime lastAlertTime) { this.lastAlertTime = lastAlertTime; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
