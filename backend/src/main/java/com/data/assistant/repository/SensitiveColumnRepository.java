package com.data.assistant.repository;

import com.data.assistant.model.SensitiveColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensitiveColumnRepository extends JpaRepository<SensitiveColumn, Long> {

    List<SensitiveColumn> findByDataSourceId(Long dataSourceId);

    List<SensitiveColumn> findByDataSourceIdAndTableName(Long dataSourceId, String tableName);

    List<SensitiveColumn> findByDataSourceIdAndTableNameAndColumnName(Long dataSourceId, String tableName, String columnName);

    @Modifying
    @Query("DELETE FROM SensitiveColumn sc WHERE sc.dataSourceId = ?1")
    void deleteByDataSourceId(Long dataSourceId);

    @Modifying
    @Query("DELETE FROM SensitiveColumn sc WHERE sc.dataSourceId = ?1 AND sc.tableName = ?2")
    void deleteByDataSourceIdAndTableName(Long dataSourceId, String tableName);

    @Query("SELECT DISTINCT sc.tableName FROM SensitiveColumn sc WHERE sc.dataSourceId = ?1")
    List<String> findDistinctTableNamesByDataSourceId(Long dataSourceId);

    long countByDataSourceId(Long dataSourceId);
}
