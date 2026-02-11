package com.data.assistant.service;

import com.data.assistant.model.Metric;
import com.data.assistant.repository.MetricRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
public class MetricService {

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private javax.sql.DataSource jdbcDataSource;

    @Transactional
    public Metric createMetric(Metric metric) {
        if (metricRepository.existsByCode(metric.getCode())) {
            throw new RuntimeException("指标编码已存在");
        }
        return metricRepository.save(metric);
    }

    @Transactional
    public Metric updateMetric(Long id, Metric metric) {
        Metric existing = metricRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("指标不存在"));
        existing.setName(metric.getName());
        existing.setDescription(metric.getDescription());
        existing.setCategory(metric.getCategory());
        existing.setAggregationType(metric.getAggregationType());
        existing.setFilterCondition(metric.getFilterCondition());
        existing.setUnit(metric.getUnit());
        existing.setIsActive(metric.getIsActive());
        return metricRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<Metric> getAllMetrics() {
        return metricRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public Metric getMetric(Long id) {
        return metricRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("指标不存在"));
    }

    @Transactional
    public void deleteMetric(Long id) {
        metricRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> calculateMetric(Long metricId) throws Exception {
        Metric metric = getMetric(metricId);

        String sql = buildMetricSql(metric);

        Map<String, Object> result = new HashMap<>();
        result.put("metricId", metricId);
        result.put("metricName", metric.getName());
        result.put("metricCode", metric.getCode());
        result.put("unit", metric.getUnit());

        try (Connection connection = jdbcDataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                result.put("value", rs.getObject(1));
            }
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> calculateMetricsByCategory(String category) {
        List<Metric> metrics = metricRepository.findByCategory(category);
        List<Map<String, Object>> results = new ArrayList<>();

        for (Metric metric : metrics) {
            try {
                Map<String, Object> result = calculateMetric(metric.getId());
                results.add(result);
            } catch (Exception e) {
                Map<String, Object> error = new HashMap<>();
                error.put("metricId", metric.getId());
                error.put("metricName", metric.getName());
                error.put("error", e.getMessage());
                results.add(error);
            }
        }

        return results;
    }

    private String buildMetricSql(Metric metric) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
           .append(metric.getAggregationType().name())
           .append("(")
           .append(metric.getColumnName())
           .append(") ")
           .append("FROM ")
           .append(metric.getTableName());

        if (metric.getFilterCondition() != null && !metric.getFilterCondition().isEmpty()) {
            sql.append(" WHERE ").append(metric.getFilterCondition());
        }

        return sql.toString();
    }

    @Transactional(readOnly = true)
    public Map<String, List<Metric>> getMetricsByCategory() {
        List<Metric> metrics = getAllMetrics();
        Map<String, List<Metric>> grouped = new HashMap<>();

        for (Metric metric : metrics) {
            String category = metric.getCategory() != null ? metric.getCategory() : "未分类";
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(metric);
        }

        return grouped;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMetricTrend(Long metricId, String timeField, int days) throws Exception {
        Metric metric = getMetric(metricId);
        List<Map<String, Object>> trend = new ArrayList<>();

        String sql = String.format(
            "SELECT DATE(%s) as date, %s(%s) as value FROM %s WHERE %s >= DATE_SUB(NOW(), INTERVAL %d DAY) GROUP BY DATE(%s) ORDER BY date",
            timeField, metric.getAggregationType().name(), metric.getColumnName(),
            metric.getTableName(), timeField, days, timeField
        );

        try (Connection connection = jdbcDataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> point = new HashMap<>();
                point.put("date", rs.getDate("date"));
                point.put("value", rs.getObject("value"));
                trend.add(point);
            }
        }

        return trend;
    }
}
