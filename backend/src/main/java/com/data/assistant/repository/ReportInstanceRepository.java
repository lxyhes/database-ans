package com.data.assistant.repository;

import com.data.assistant.model.ReportInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportInstanceRepository extends JpaRepository<ReportInstance, Long> {

    List<ReportInstance> findByTemplateIdOrderByExecuteTimeDesc(Long templateId);

    List<ReportInstance> findByTemplateIdAndStatusOrderByExecuteTimeDesc(Long templateId, ReportInstance.ExecuteStatus status);

    long countByTemplateId(Long templateId);
}
