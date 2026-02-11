package com.data.assistant.service;

import com.data.assistant.model.DataLineage;
import com.data.assistant.repository.DataLineageRepository;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class DataLineageService {

    @Autowired
    private DataLineageRepository dataLineageRepository;

    @Transactional
    public void recordQueryLineage(Long dataSourceId, String sql, String createdBy) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                Select select = (Select) statement;
                List<DataLineage> lineages = extractLineageFromSelect(dataSourceId, select, sql, createdBy);
                for (DataLineage lineage : lineages) {
                    dataLineageRepository.save(lineage);
                }
            }
        } catch (Exception e) {
            // 解析失败不影响主流程
            System.err.println("Failed to parse SQL for lineage: " + e.getMessage());
        }
    }

    private List<DataLineage> extractLineageFromSelect(Long dataSourceId, Select select, String sql, String createdBy) {
        List<DataLineage> lineages = new ArrayList<>();

        if (select.getSelectBody() instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) select.getSelectBody();

            // 获取目标表（FROM和JOIN）
            List<String> targetTables = extractTargetTables(plainSelect);

            // 获取源表（子查询或实际表）
            List<String> sourceTables = extractSourceTables(plainSelect);

            // 获取SELECT项
            List<SelectItem> selectItems = plainSelect.getSelectItems();

            for (SelectItem item : selectItems) {
                if (item instanceof SelectExpressionItem) {
                    SelectExpressionItem exprItem = (SelectExpressionItem) item;
                    String alias = exprItem.getAlias() != null ? exprItem.getAlias().getName() : null;

                    // 提取列引用
                    List<String> columns = extractColumns(exprItem.getExpression());

                    for (String targetTable : targetTables) {
                        for (String sourceTable : sourceTables) {
                            for (String column : columns) {
                                DataLineage lineage = new DataLineage();
                                lineage.setDataSourceId(dataSourceId);
                                lineage.setSourceTable(sourceTable);
                                lineage.setSourceColumn(column);
                                lineage.setTargetTable(targetTable);
                                lineage.setTargetColumn(alias != null ? alias : column);
                                lineage.setTransformationType(detectTransformationType(exprItem.getExpression()));
                                lineage.setTransformationLogic(exprItem.getExpression().toString());
                                lineage.setLineageType(DataLineage.LineageType.QUERY);
                                lineage.setSqlQuery(sql);
                                lineage.setCreatedBy(createdBy);
                                lineages.add(lineage);
                            }
                        }
                    }
                }
            }
        }

        return lineages;
    }

    private List<String> extractTargetTables(PlainSelect plainSelect) {
        List<String> tables = new ArrayList<>();

        // FROM子句
        if (plainSelect.getFromItem() instanceof Table) {
            tables.add(((Table) plainSelect.getFromItem()).getName());
        }

        // JOIN子句
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                if (join.getRightItem() instanceof Table) {
                    tables.add(((Table) join.getRightItem()).getName());
                }
            }
        }

        return tables;
    }

    private List<String> extractSourceTables(PlainSelect plainSelect) {
        List<String> tables = new ArrayList<>();

        if (plainSelect.getFromItem() instanceof Table) {
            tables.add(((Table) plainSelect.getFromItem()).getName());
        }

        return tables.isEmpty() ? Arrays.asList("unknown") : tables;
    }

    private List<String> extractColumns(net.sf.jsqlparser.expression.Expression expression) {
        List<String> columns = new ArrayList<>();

        if (expression instanceof Column) {
            columns.add(((Column) expression).getColumnName());
        }

        return columns.isEmpty() ? Arrays.asList("*") : columns;
    }

    private DataLineage.TransformationType detectTransformationType(net.sf.jsqlparser.expression.Expression expression) {
        String exprStr = expression.toString().toUpperCase();

        if (exprStr.contains("SUM(") || exprStr.contains("COUNT(") || exprStr.contains("AVG(") ||
            exprStr.contains("MAX(") || exprStr.contains("MIN(")) {
            return DataLineage.TransformationType.AGGREGATION;
        }
        if (exprStr.contains("+") || exprStr.contains("-") || exprStr.contains("*") || exprStr.contains("/")) {
            return DataLineage.TransformationType.CALCULATION;
        }
        if (exprStr.contains("CASE") || exprStr.contains("DECODE")) {
            return DataLineage.TransformationType.CALCULATION;
        }

        return DataLineage.TransformationType.SELECT;
    }

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
