package com.data.assistant.repository;

import com.data.assistant.model.DataLineage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataLineageRepository extends JpaRepository<DataLineage, Long> {

    List<DataLineage> findByDataSourceId(Long dataSourceId);

    List<DataLineage> findByDataSourceIdAndSourceTable(Long dataSourceId, String sourceTable);

    List<DataLineage> findByDataSourceIdAndTargetTable(Long dataSourceId, String targetTable);

    List<DataLineage> findByDataSourceIdAndSourceTableAndSourceColumn(Long dataSourceId, String sourceTable, String sourceColumn);

    List<DataLineage> findByDataSourceIdAndTargetTableAndTargetColumn(Long dataSourceId, String targetTable, String targetColumn);

    @Query("SELECT DISTINCT dl.sourceTable FROM DataLineage dl WHERE dl.dataSourceId = ?1")
    List<String> findDistinctSourceTablesByDataSourceId(Long dataSourceId);

    @Query("SELECT DISTINCT dl.targetTable FROM DataLineage dl WHERE dl.dataSourceId = ?1")
    List<String> findDistinctTargetTablesByDataSourceId(Long dataSourceId);
}
