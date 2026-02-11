package com.data.assistant.service;

import com.data.assistant.model.DataSource;
import com.data.assistant.model.TableRelation;
import com.data.assistant.repository.DataSourceRepository;
import com.data.assistant.repository.TableRelationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource as JdbcDataSource;
import java.sql.*;
import java.util.*;

@Service
public class TableRelationService {

    @Autowired
    private TableRelationRepository tableRelationRepository;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private javax.sql.DataSource jdbcDataSource;

    @Transactional
    public void analyzeTableRelations(Long dataSourceId) {
        DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new RuntimeException("DataSource not found"));

        // 清除旧的关系数据
        tableRelationRepository.deleteByDataSourceId(dataSourceId);

        try (Connection connection = getConnection(dataSource)) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            String schema = connection.getSchema();

            // 1. 分析外键关系
            analyzeForeignKeys(metaData, catalog, schema, dataSourceId);

            // 2. 分析字段命名推断关系
            analyzeInferredRelations(metaData, catalog, schema, dataSourceId);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to analyze table relations", e);
        }
    }

    private void analyzeForeignKeys(DatabaseMetaData metaData, String catalog, String schema, Long dataSourceId)
            throws SQLException {

        // 获取所有表
        List<String> tables = new ArrayList<>();
        try (ResultSet rs = metaData.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }

        // 分析每个表的外键
        for (String table : tables) {
            try (ResultSet rs = metaData.getImportedKeys(catalog, schema, table)) {
                while (rs.next()) {
                    String pkTable = rs.getString("PKTABLE_NAME");
                    String pkColumn = rs.getString("PKCOLUMN_NAME");
                    String fkColumn = rs.getString("FKCOLUMN_NAME");

                    TableRelation relation = new TableRelation();
                    relation.setDataSourceId(dataSourceId);
                    relation.setSourceTable(table);
                    relation.setSourceColumn(fkColumn);
                    relation.setTargetTable(pkTable);
                    relation.setTargetColumn(pkColumn);
                    relation.setRelationType(TableRelation.RelationType.FOREIGN_KEY);
                    relation.setConfidence(1.0);
                    relation.setDescription(String.format("外键关系: %s.%s -> %s.%s",
                            table, fkColumn, pkTable, pkColumn));

                    tableRelationRepository.save(relation);
                }
            }
        }
    }

    private void analyzeInferredRelations(DatabaseMetaData metaData, String catalog, String schema, Long dataSourceId)
            throws SQLException {

        // 获取所有表和字段
        Map<String, List<String>> tableColumns = new HashMap<>();

        try (ResultSet rs = metaData.getColumns(catalog, schema, "%", "%")) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                String columnName = rs.getString("COLUMN_NAME");
                tableColumns.computeIfAbsent(tableName, k -> new ArrayList<>()).add(columnName);
            }
        }

        // 推断关系：user_id -> users.id 模式
        for (Map.Entry<String, List<String>> entry : tableColumns.entrySet()) {
            String tableName = entry.getKey();
            List<String> columns = entry.getValue();

            for (String column : columns) {
                // 检查是否是外键字段命名模式
                if (column.endsWith("_id") && !column.equals("id")) {
                    String potentialTable = column.substring(0, column.length() - 3);

                    // 检查是否存在对应的表
                    for (String targetTable : tableColumns.keySet()) {
                        if (targetTable.toLowerCase().equals(potentialTable.toLowerCase()) ||
                                targetTable.toLowerCase().equals(potentialTable.toLowerCase() + "s")) {

                            List<String> targetColumns = tableColumns.get(targetTable);
                            if (targetColumns.contains("id")) {
                                // 检查是否已存在外键关系
                                Optional<TableRelation> existing = tableRelationRepository
                                        .findByDataSourceIdAndSourceTableAndSourceColumnAndTargetTableAndTargetColumn(
                                                dataSourceId, tableName, column, targetTable, "id");

                                if (!existing.isPresent()) {
                                    TableRelation relation = new TableRelation();
                                    relation.setDataSourceId(dataSourceId);
                                    relation.setSourceTable(tableName);
                                    relation.setSourceColumn(column);
                                    relation.setTargetTable(targetTable);
                                    relation.setTargetColumn("id");
                                    relation.setRelationType(TableRelation.RelationType.INFERRED);
                                    relation.setConfidence(0.7);
                                    relation.setDescription(String.format("推断关系: %s.%s 可能关联 %s.id",
                                            tableName, column, targetTable));

                                    tableRelationRepository.save(relation);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<TableRelation> getTableRelations(Long dataSourceId) {
        return tableRelationRepository.findByDataSourceId(dataSourceId);
    }

    @Transactional(readOnly = true)
    public List<TableRelation> getTableRelationsForTable(Long dataSourceId, String tableName) {
        List<TableRelation> relations = new ArrayList<>();
        relations.addAll(tableRelationRepository.findByDataSourceIdAndSourceTable(dataSourceId, tableName));
        relations.addAll(tableRelationRepository.findByDataSourceIdAndTargetTable(dataSourceId, tableName));
        return relations;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRelationGraphData(Long dataSourceId) {
        List<TableRelation> relations = tableRelationRepository.findByDataSourceId(dataSourceId);

        Set<String> tableNames = new HashSet<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();

        for (TableRelation relation : relations) {
            tableNames.add(relation.getSourceTable());
            tableNames.add(relation.getTargetTable());

            Map<String, Object> link = new HashMap<>();
            link.put("source", relation.getSourceTable());
            link.put("target", relation.getTargetTable());
            link.put("sourceColumn", relation.getSourceColumn());
            link.put("targetColumn", relation.getTargetColumn());
            link.put("relationType", relation.getRelationType());
            link.put("confidence", relation.getConfidence());
            links.add(link);
        }

        for (String tableName : tableNames) {
            Map<String, Object> node = new HashMap<>();
            node.put("name", tableName);
            node.put("category", getTableCategory(tableName));
            nodes.add(node);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("links", links);
        result.put("totalTables", tableNames.size());
        result.put("totalRelations", relations.size());

        return result;
    }

    private String getTableCategory(String tableName) {
        String lower = tableName.toLowerCase();
        if (lower.contains("user") || lower.contains("account")) return "用户";
        if (lower.contains("order") || lower.contains("sale")) return "订单";
        if (lower.contains("product") || lower.contains("goods")) return "商品";
        if (lower.contains("category") || lower.contains("type")) return "分类";
        if (lower.contains("log") || lower.contains("record")) return "日志";
        return "其他";
    }

    private Connection getConnection(DataSource dataSource) throws SQLException {
        // 使用默认数据源连接，实际项目中可能需要根据数据源配置动态创建连接
        return jdbcDataSource.getConnection();
    }

    @Transactional
    public TableRelation addManualRelation(Long dataSourceId, String sourceTable, String sourceColumn,
                                           String targetTable, String targetColumn, String description) {
        TableRelation relation = new TableRelation();
        relation.setDataSourceId(dataSourceId);
        relation.setSourceTable(sourceTable);
        relation.setSourceColumn(sourceColumn);
        relation.setTargetTable(targetTable);
        relation.setTargetColumn(targetColumn);
        relation.setRelationType(TableRelation.RelationType.MANUAL);
        relation.setConfidence(1.0);
        relation.setDescription(description);

        return tableRelationRepository.save(relation);
    }

    @Transactional
    public void deleteRelation(Long relationId) {
        tableRelationRepository.deleteById(relationId);
    }
}
