package com.data.assistant.service;

import com.data.assistant.model.DataSource;
import com.data.assistant.repository.DataSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 数据源管理服务
 */
@Service
public class DataSourceService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSourceService.class);
    
    @Autowired
    private DataSourceRepository dataSourceRepository;
    
    @Autowired
    @Lazy
    private DynamicDataSourceService dynamicDataSourceService;
    
    /**
     * 获取所有数据源
     */
    public List<DataSource> getAllDataSources() {
        List<DataSource> sources = dataSourceRepository.findAll();
        // 过滤掉 H2 数据源，并检查每个数据源的连接状态
        return sources.stream()
            .filter(source -> !"h2".equalsIgnoreCase(source.getType()))
            .peek(source -> source.setConnectionStatus(checkConnectionStatus(source)))
            .collect(Collectors.toList());
    }
    
    /**
     * 获取激活的数据源
     */
    public List<DataSource> getActiveDataSources() {
        return dataSourceRepository.findByActiveTrue();
    }
    
    /**
     * 根据 ID 获取数据源
     */
    public Optional<DataSource> getDataSourceById(Long id) {
        return dataSourceRepository.findById(id);
    }
    
    /**
     * 获取默认数据源
     */
    public Optional<DataSource> getDefaultDataSource() {
        return dataSourceRepository.findByIsDefaultTrue();
    }
    
    /**
     * 创建数据源
     */
    public DataSource createDataSource(DataSource dataSource) {
        // 检查名称是否已存在
        if (dataSourceRepository.existsByName(dataSource.getName())) {
            throw new IllegalArgumentException("数据源名称已存在");
        }
        
        // 测试连接
        if (!testConnection(dataSource)) {
            throw new IllegalArgumentException("数据库连接失败，请检查配置");
        }
        
        // 如果是第一个数据源或设置为默认，则设为默认
        if (dataSource.getIsDefault() || dataSourceRepository.count() == 0) {
            // 取消其他默认数据源
            dataSourceRepository.findByIsDefaultTrue().ifPresent(ds -> {
                ds.setIsDefault(false);
                dataSourceRepository.save(ds);
            });
            dataSource.setIsDefault(true);
        }
        
        dataSource.setLastConnectedAt(LocalDateTime.now());
        DataSource saved = dataSourceRepository.save(dataSource);
        
        // 初始化到动态数据源
        try {
            dynamicDataSourceService.switchDataSource(saved.getId());
        } catch (Exception e) {
            logger.warn("Failed to initialize datasource in pool: {}", e.getMessage());
        }
        
        return saved;
    }
    
    /**
     * 更新数据源
     */
    public DataSource updateDataSource(Long id, DataSource dataSource) {
        DataSource existing = dataSourceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("数据源不存在"));
        
        // 检查名称是否冲突
        if (!existing.getName().equals(dataSource.getName()) && 
            dataSourceRepository.existsByName(dataSource.getName())) {
            throw new IllegalArgumentException("数据源名称已存在");
        }
        
        // 更新字段
        existing.setName(dataSource.getName());
        existing.setType(dataSource.getType());
        existing.setHost(dataSource.getHost());
        existing.setPort(dataSource.getPort());
        existing.setDatabase(dataSource.getDatabase());
        existing.setUsername(dataSource.getUsername());
        if (dataSource.getPassword() != null && !dataSource.getPassword().isEmpty()) {
            existing.setPassword(dataSource.getPassword());
        }
        existing.setConnectionParams(dataSource.getConnectionParams());
        existing.setDescription(dataSource.getDescription());
        existing.setActive(dataSource.getActive());
        
        // 处理默认数据源切换
        if (dataSource.getIsDefault() && !existing.getIsDefault()) {
            dataSourceRepository.findByIsDefaultTrue().ifPresent(ds -> {
                ds.setIsDefault(false);
                dataSourceRepository.save(ds);
            });
            existing.setIsDefault(true);
        }
        
        DataSource updated = dataSourceRepository.save(existing);
        
        // 刷新动态数据源
        dynamicDataSourceService.refreshDataSource(id);
        
        return updated;
    }
    
    /**
     * 删除数据源
     */
    public void deleteDataSource(Long id) {
        DataSource dataSource = dataSourceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("数据源不存在"));
        
        // 如果删除的是默认数据源，需要重新设置默认
        if (dataSource.getIsDefault()) {
            List<DataSource> others = dataSourceRepository.findByActiveTrue();
            others.remove(dataSource);
            if (!others.isEmpty()) {
                DataSource newDefault = others.get(0);
                newDefault.setIsDefault(true);
                dataSourceRepository.save(newDefault);
            }
        }
        
        // 从动态数据源移除
        dynamicDataSourceService.removeDataSource(id);
        
        dataSourceRepository.delete(dataSource);
    }
    
    /**
     * 测试数据源连接
     */
    public boolean testConnection(DataSource dataSource) {
        String jdbcUrl = buildJdbcUrl(dataSource);
        
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl, 
                dataSource.getUsername(), 
                dataSource.getPassword())) {
            return connection.isValid(5);
        } catch (Exception e) {
            logger.error("Connection test failed for datasource: {}", dataSource.getName(), e);
            return false;
        }
    }
    
    /**
     * 检查连接状态
     */
    private String checkConnectionStatus(DataSource dataSource) {
        return testConnection(dataSource) ? "connected" : "disconnected";
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
                return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", 
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
     * 设置默认数据源
     */
    public void setDefaultDataSource(Long id) {
        DataSource newDefault = dataSourceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("数据源不存在"));
        
        // 取消其他默认
        dataSourceRepository.findByIsDefaultTrue().ifPresent(ds -> {
            ds.setIsDefault(false);
            dataSourceRepository.save(ds);
        });
        
        newDefault.setIsDefault(true);
        dataSourceRepository.save(newDefault);
    }
    
    /**
     * 获取数据源的所有表
     */
    public List<Map<String, Object>> getDataSourceTables(Long dataSourceId) {
        DataSource dataSource = dataSourceRepository.findById(dataSourceId)
            .orElseThrow(() -> new IllegalArgumentException("数据源不存在"));
        
        try {
            JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate(dataSourceId);
            
            String sql;
            String type = dataSource.getType().toLowerCase();
            
            switch (type) {
                case "mysql":
                    sql = "SELECT table_name as name, table_comment as comment, table_rows as rowCount " +
                          "FROM information_schema.tables " +
                          "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'";
                    break;
                case "postgresql":
                    sql = "SELECT table_name as name, '' as comment, 0 as rowCount " +
                          "FROM information_schema.tables " +
                          "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'";
                    break;
                case "h2":
                    sql = "SELECT table_name as name, '' as comment, 0 as rowCount " +
                          "FROM information_schema.tables " +
                          "WHERE table_schema = 'PUBLIC'";
                    break;
                default:
                    throw new IllegalArgumentException("不支持的数据库类型: " + type);
            }
            
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            logger.error("Failed to get tables for datasource {}", dataSourceId, e);
            throw new RuntimeException("获取表列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取表的列信息
     */
    public List<Map<String, Object>> getTableColumns(Long dataSourceId, String tableName) {
        DataSource dataSource = dataSourceRepository.findById(dataSourceId)
            .orElseThrow(() -> new IllegalArgumentException("数据源不存在"));
        
        try {
            JdbcTemplate jdbcTemplate = dynamicDataSourceService.getJdbcTemplate(dataSourceId);
            
            String sql;
            String type = dataSource.getType().toLowerCase();
            
            switch (type) {
                case "mysql":
                    sql = String.format(
                        "SELECT column_name as name, data_type as type, column_comment as comment, " +
                        "is_nullable as nullable, column_default as defaultValue " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = '%s' " +
                        "ORDER BY ordinal_position", tableName);
                    break;
                case "postgresql":
                    sql = String.format(
                        "SELECT column_name as name, data_type as type, '' as comment, " +
                        "is_nullable as nullable, column_default as defaultValue " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = 'public' AND table_name = '%s' " +
                        "ORDER BY ordinal_position", tableName);
                    break;
                case "h2":
                    sql = String.format(
                        "SELECT column_name as name, type_name as type, '' as comment, " +
                        "is_nullable as nullable, column_default as defaultValue " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = 'PUBLIC' AND table_name = '%s' " +
                        "ORDER by ordinal_position", tableName);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的数据库类型: " + type);
            }
            
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            logger.error("Failed to get columns for table {} in datasource {}", tableName, dataSourceId, e);
            throw new RuntimeException("获取列信息失败: " + e.getMessage());
        }
    }
}
