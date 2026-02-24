package com.data.assistant.repository;

import com.data.assistant.model.AlertRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {
    
    List<AlertRecord> findByStatus(String status);
    
    List<AlertRecord> findByRuleId(Long ruleId);
    
    List<AlertRecord> findByDataSourceId(Long dataSourceId);
    
    List<AlertRecord> findByAlertLevel(String alertLevel);
    
    @Query("SELECT a FROM AlertRecord a WHERE a.createdAt >= :startTime ORDER BY a.createdAt DESC")
    List<AlertRecord> findRecentAlerts(@Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT COUNT(a) FROM AlertRecord a WHERE a.status = 'PENDING'")
    Long countPendingAlerts();
    
    @Query("SELECT COUNT(a) FROM AlertRecord a WHERE a.alertLevel = 'CRITICAL' AND a.status = 'PENDING'")
    Long countCriticalPendingAlerts();
    
    @Query("SELECT COUNT(a) FROM AlertRecord a WHERE a.createdAt >= :startTime")
    Long countAlertsSince(@Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT a FROM AlertRecord a WHERE a.createdAt >= :startTime AND a.alertLevel = :level ORDER BY a.createdAt DESC")
    List<AlertRecord> findByLevelAndTime(@Param("level") String level, @Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT new map(a.status as status, COUNT(a) as count) FROM AlertRecord a GROUP BY a.status")
    List<Object[]> countByStatus();
    
    @Query("SELECT new map(a.alertLevel as level, COUNT(a) as count) FROM AlertRecord a WHERE a.createdAt >= :startTime GROUP BY a.alertLevel")
    List<Object[]> countByLevelSince(@Param("startTime") LocalDateTime startTime);
}
