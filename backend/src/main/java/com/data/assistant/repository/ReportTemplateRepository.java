package com.data.assistant.repository;

import com.data.assistant.model.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {

    List<ReportTemplate> findByDataSourceId(Long dataSourceId);

    List<ReportTemplate> findByIsActiveTrue();

    List<ReportTemplate> findByDataSourceIdAndIsActiveTrue(Long dataSourceId);
}
