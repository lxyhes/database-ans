package com.data.assistant.repository;

import com.data.assistant.model.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据源仓库
 */
@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, Long> {
    
    /**
     * 查找所有激活的数据源
     */
    List<DataSource> findByActiveTrue();
    
    /**
     * 查找默认数据源
     */
    Optional<DataSource> findByIsDefaultTrue();
    
    /**
     * 按类型查找数据源
     */
    List<DataSource> findByType(String type);
    
    /**
     * 检查名称是否存在
     */
    boolean existsByName(String name);
}
