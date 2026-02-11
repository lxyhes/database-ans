package com.data.assistant.service;

import com.data.assistant.model.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态数据源服务
 * 管理多个数据源的连接池
 */
@Service
public class DynamicDataSourceService {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamicDataSourceService.class);
    
    @Autowired
    private DataSourceService dataSourceService;
    
    // 数据源连接池缓存
    private final Map<Long, HikariDataSource> dataSourcePool = new ConcurrentHashMap<>();
    
    // 当前使用的数据源ID
    private Long currentDataSourceId = null;
    
    @PostConstruct
    public void init() {
        // 加载默认数据源
        dataSourceService.getDefaultDataSource().ifPresent(ds -> {
            try {
                switchDataSource(ds.getId());
                logger.info("Default datasource initialized: {}", ds.getName());
            } catch (Exception e) {
                logger.error("Failed to initialize default datasource", e);
            }
        });
    }
    
    @PreDestroy
    public void destroy() {
        // 关闭所有连接池
        dataSourcePool.forEach((id, ds) -> {
            try {
                ds.close();
                logger.info("Closed datasource pool: {}", id);
            } catch (Exception e) {
                logger.error("Failed to close datasource pool: {}", id, e);
            }
        });
    }
    
    /**
     * 切换数据源
     */
    public void switchDataSource(Long dataSourceId) {
        if (dataSourceId.equals(currentDataSourceId)) {
            return; // 已经是当前数据源
        }
        
        DataSource dataSource = dataSourceService.getDataSourceById(dataSourceId)
            .orElseThrow(() -> new IllegalArgumentException("数据源不存在: " + dataSourceId));
        
        // 获取或创建连接池
        HikariDataSource hikariDS = dataSourcePool.computeIfAbsent(dataSourceId, id -> {
            return createHikariDataSource(dataSource);
        });
        
        // 测试连接
        try (Connection conn = hikariDS.getConnection()) {
            if (!conn.isValid(5)) {
                throw new SQLException("数据源连接无效");
            }
        } catch (SQLException e) {
            // 移除无效的连接池
            dataSourcePool.remove(dataSourceId);
            throw new RuntimeException("无法连接到数据源: " + e.getMessage(), e);
        }
        
        currentDataSourceId = dataSourceId;
        logger.info("Switched to datasource: {} (ID: {})", dataSource.getName(), dataSourceId);
    }
    
    /**
     * 获取当前 JdbcTemplate
     */
    public JdbcTemplate getCurrentJdbcTemplate() {
        if (currentDataSourceId == null) {
            throw new IllegalStateException("未选择数据源，请先配置并选择数据源");
        }
        
        HikariDataSource dataSource = dataSourcePool.get(currentDataSourceId);
        if (dataSource == null) {
            throw new IllegalStateException("当前数据源连接池不存在");
        }
        
        return new JdbcTemplate(dataSource);
    }
    
    /**
     * 获取指定数据源的 JdbcTemplate
     */
    public JdbcTemplate getJdbcTemplate(Long dataSourceId) {
        switchDataSource(dataSourceId);
        return getCurrentJdbcTemplate();
    }
    
    /**
     * 获取当前数据源ID
     */
    public Long getCurrentDataSourceId() {
        return currentDataSourceId;
    }
    
    /**
     * 创建 Hikari 连接池
     */
    private HikariDataSource createHikariDataSource(DataSource dataSource) {
        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl(buildJdbcUrl(dataSource));
        config.setUsername(dataSource.getUsername());
        config.setPassword(dataSource.getPassword());
        config.setDriverClassName(getDriverClassName(dataSource.getType()));
        
        // 连接池配置
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(20000);
        config.setMaxLifetime(1200000);
        
        // 连接测试
        config.setConnectionTestQuery("SELECT 1");
        
        return new HikariDataSource(config);
    }
    
    /**
     * 构建 JDBC URL
     */
    private String buildJdbcUrl(DataSource dataSource) {
        String type = dataSource.getType().toLowerCase();
        String host = dataSource.getHost();
        int port = dataSource.getPort();
        String database = dataSource.getDatabase();
        
        switch (type) {
            case "mysql":
                return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true", 
                    host, port, database);
            case "postgresql":
                return String.format("jdbc:postgresql://%s:%d/%s", 
                    host, port, database);
            case "h2":
                return String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1", database);
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + type);
        }
    }
    
    /**
     * 获取驱动类名
     */
    private String getDriverClassName(String type) {
        switch (type.toLowerCase()) {
            case "mysql":
                return "com.mysql.cj.jdbc.Driver";
            case "postgresql":
                return "org.postgresql.Driver";
            case "h2":
                return "org.h2.Driver";
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + type);
        }
    }
    
    /**
     * 刷新数据源（配置变更后调用）
     */
    public void refreshDataSource(Long dataSourceId) {
        HikariDataSource oldDS = dataSourcePool.remove(dataSourceId);
        if (oldDS != null && !oldDS.isClosed()) {
            oldDS.close();
        }
        
        // 如果是当前数据源，重新初始化
        if (dataSourceId.equals(currentDataSourceId)) {
            switchDataSource(dataSourceId);
        }
        
        logger.info("Refreshed datasource: {}", dataSourceId);
    }
    
    /**
     * 移除数据源
     */
    public void removeDataSource(Long dataSourceId) {
        HikariDataSource ds = dataSourcePool.remove(dataSourceId);
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
        
        if (dataSourceId.equals(currentDataSourceId)) {
            currentDataSourceId = null;
        }
        
        logger.info("Removed datasource: {}", dataSourceId);
    }
}
