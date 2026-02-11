package com.data.assistant.repository;

import com.data.assistant.model.Metric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {

    List<Metric> findByCategory(String category);

    List<Metric> findByIsActiveTrue();

    List<Metric> findByDataSourceId(Long dataSourceId);

    Optional<Metric> findByCode(String code);

    boolean existsByCode(String code);
}
