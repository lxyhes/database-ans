package com.data.assistant.service;

import com.data.assistant.model.DataLineage;
import com.data.assistant.repository.DataLineageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class DataLineageService {

    @Autowired
    private DataLineageRepository dataLineageRepository;

    @Transactional
    public DataLineage addManualLineage(DataLineage lineage) {
        lineage.setLineageType(DataLineage.LineageType.MANUAL);
        return dataLineageRepository.save(lineage);
    }

    @Transactional(readOnly = true)
    public List<DataLineage> getLineages(Long dataSourceId) {
        return dataLineageRepository.findByDataSourceId(dataSourceId);
    }

    @Transactional(readOnly = true)
    public List<DataLineage> getTableLineages(Long dataSourceId, String tableName) {
        List<DataLineage> lineages = new ArrayList<>();
        lineages.addAll(dataLineageRepository.findByDataSourceIdAndSourceTable(dataSourceId, tableName));
        lineages.addAll(dataLineageRepository.findByDataSourceIdAndTargetTable(dataSourceId, tableName));
        return lineages;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLineageGraph(Long dataSourceId, String tableName) {
        List<DataLineage> lineages = getTableLineages(dataSourceId, tableName);

        Set<String> tableNames = new HashSet<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();

        for (DataLineage lineage : lineages) {
            tableNames.add(lineage.getSourceTable());
            tableNames.add(lineage.getTargetTable());

            Map<String, Object> link = new HashMap<>();
            link.put("source", lineage.getSourceTable());
            link.put("target", lineage.getTargetTable());
            link.put("sourceColumn", lineage.getSourceColumn());
            link.put("targetColumn", lineage.getTargetColumn());
            link.put("transformationType", lineage.getTransformationType());
            link.put("lineageType", lineage.getLineageType());
            links.add(link);
        }

        for (String name : tableNames) {
            Map<String, Object> node = new HashMap<>();
            node.put("name", name);
            node.put("category", name.equals(tableName) ? "current" : "related");
            nodes.add(node);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("links", links);
        result.put("totalTables", tableNames.size());
        result.put("totalLineages", lineages.size());

        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> traceUpstream(Long dataSourceId, String tableName, String columnName) {
        List<Map<String, Object>> upstream = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        traceUpstreamRecursive(dataSourceId, tableName, columnName, upstream, visited, 0);
        return upstream;
    }

    private void traceUpstreamRecursive(Long dataSourceId, String tableName, String columnName,
                                        List<Map<String, Object>> result, Set<String> visited, int depth) {
        String key = tableName + "." + columnName;
        if (visited.contains(key) || depth > 10) return;
        visited.add(key);

        List<DataLineage> lineages = dataLineageRepository
                .findByDataSourceIdAndTargetTableAndTargetColumn(dataSourceId, tableName, columnName);

        for (DataLineage lineage : lineages) {
            Map<String, Object> item = new HashMap<>();
            item.put("table", lineage.getSourceTable());
            item.put("column", lineage.getSourceColumn());
            item.put("transformationType", lineage.getTransformationType());
            item.put("transformationLogic", lineage.getTransformationLogic());
            item.put("depth", depth);
            result.add(item);

            traceUpstreamRecursive(dataSourceId, lineage.getSourceTable(), lineage.getSourceColumn(),
                    result, visited, depth + 1);
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> traceDownstream(Long dataSourceId, String tableName, String columnName) {
        List<Map<String, Object>> downstream = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        traceDownstreamRecursive(dataSourceId, tableName, columnName, downstream, visited, 0);
        return downstream;
    }

    private void traceDownstreamRecursive(Long dataSourceId, String tableName, String columnName,
                                          List<Map<String, Object>> result, Set<String> visited, int depth) {
        String key = tableName + "." + columnName;
        if (visited.contains(key) || depth > 10) return;
        visited.add(key);

        List<DataLineage> lineages = dataLineageRepository
                .findByDataSourceIdAndSourceTableAndSourceColumn(dataSourceId, tableName, columnName);

        for (DataLineage lineage : lineages) {
            Map<String, Object> item = new HashMap<>();
            item.put("table", lineage.getTargetTable());
            item.put("column", lineage.getTargetColumn());
            item.put("transformationType", lineage.getTransformationType());
            item.put("transformationLogic", lineage.getTransformationLogic());
            item.put("depth", depth);
            result.add(item);

            traceDownstreamRecursive(dataSourceId, lineage.getTargetTable(), lineage.getTargetColumn(),
                    result, visited, depth + 1);
        }
    }

    @Transactional
    public void deleteLineage(Long id) {
        dataLineageRepository.deleteById(id);
    }
}
