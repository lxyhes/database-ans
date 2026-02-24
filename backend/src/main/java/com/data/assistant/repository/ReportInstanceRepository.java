package com.data.assistant.repository;

import com.data.assistant.model.ReportInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportInstanceRepository extends JpaRepository<ReportInstance, Long> {
    
    List<ReportInstance> findByDataSourceIdOrderByCreatedAtDesc(Long dataSourceId);
    
    List<ReportInstance> findAllByOrderByCreatedAtDesc();
}
