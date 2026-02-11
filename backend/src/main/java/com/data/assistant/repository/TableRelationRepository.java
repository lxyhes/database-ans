package com.data.assistant.repository;

import com.data.assistant.model.TableRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TableRelationRepository extends JpaRepository<TableRelation, Long> {

    List<TableRelation> findByDataSourceId(Long dataSourceId);

    List<TableRelation> findByDataSourceIdAndSourceTable(Long dataSourceId, String sourceTable);

    List<TableRelation> findByDataSourceIdAndTargetTable(Long dataSourceId, String targetTable);

    Optional<TableRelation> findByDataSourceIdAndSourceTableAndSourceColumnAndTargetTableAndTargetColumn(
            Long dataSourceId, String sourceTable, String sourceColumn, String targetTable, String targetColumn);

    @Modifying
    @Query("DELETE FROM TableRelation tr WHERE tr.dataSourceId = ?1")
    void deleteByDataSourceId(Long dataSourceId);

    @Query("SELECT DISTINCT tr.sourceTable FROM TableRelation tr WHERE tr.dataSourceId = ?1")
    List<String> findDistinctSourceTablesByDataSourceId(Long dataSourceId);

    @Query("SELECT DISTINCT tr.targetTable FROM TableRelation tr WHERE tr.dataSourceId = ?1")
    List<String> findDistinctTargetTablesByDataSourceId(Long dataSourceId);
}
