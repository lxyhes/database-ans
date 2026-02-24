package com.data.assistant.repository;

import com.data.assistant.model.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {
    
    List<AlertRule> findByIsActiveTrue();
    
    List<AlertRule> findByDataSourceId(Long dataSourceId);
    
    List<AlertRule> findByDataSourceIdAndIsActiveTrue(Long dataSourceId);
    
    @Query("SELECT r FROM AlertRule r WHERE r.isActive = true AND " +
           "(r.lastCheckTime IS NULL OR r.lastCheckTime < :threshold)")
    List<AlertRule> findRulesNeedingCheck(LocalDateTime threshold);
    
    @Query("SELECT r FROM AlertRule r WHERE r.isActive = true AND r.lastAlertTime > :since")
    List<AlertRule> findRecentlyTriggered(LocalDateTime since);
}
