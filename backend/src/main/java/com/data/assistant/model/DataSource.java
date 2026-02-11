package com.data.assistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 数据源实体
 */
@Entity
@Table(name = "data_sources")
public class DataSource {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 数据源名称
     */
    @Column(nullable = false)
    private String name;
    
    /**
     * 数据源类型（mysql, postgresql, h2 等）
     */
    @Column(nullable = false)
    private String type;
    
    /**
     * 主机地址
     */
    @Column(nullable = false)
    private String host;
    
    /**
     * 端口
     */
    @Column(nullable = false)
    private Integer port;
    
    /**
     * 数据库名称
     */
    @Column(nullable = false)
    private String database;
    
    /**
     * 用户名
     */
    @Column(nullable = false)
    private String username;
    
    /**
     * 密码（加密存储）
     */
    @Column(nullable = false)
    private String password;
    
    /**
     * 连接参数（JSON 格式）
     */
    private String connectionParams;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 是否激活
     */
    @Column(nullable = false)
    private Boolean active = true;
    
    /**
     * 是否默认数据源
     */
    @Column(nullable = false)
    private Boolean isDefault = false;
    
    /**
     * 最后连接时间
     */
    private LocalDateTime lastConnectedAt;
    
    /**
     * 连接状态
     */
    @Transient
    private String connectionStatus;
    
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
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
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public Integer getPort() {
        return port;
    }
    
    public void setPort(Integer port) {
        this.port = port;
    }
    
    public String getDatabase() {
        return database;
    }
    
    public void setDatabase(String database) {
        this.database = database;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getConnectionParams() {
        return connectionParams;
    }
    
    public void setConnectionParams(String connectionParams) {
        this.connectionParams = connectionParams;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    public Boolean getIsDefault() {
        return isDefault;
    }
    
    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
    
    public LocalDateTime getLastConnectedAt() {
        return lastConnectedAt;
    }
    
    public void setLastConnectedAt(LocalDateTime lastConnectedAt) {
        this.lastConnectedAt = lastConnectedAt;
    }
    
    public String getConnectionStatus() {
        return connectionStatus;
    }
    
    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus = connectionStatus;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
