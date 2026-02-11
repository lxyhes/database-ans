package com.data.assistant.service;

import com.data.assistant.repository.DataSourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
public class FederationQueryService {

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private DataSource jdbcDataSource;

    public Map<String, Object> executeFederationQuery(List<Long> dataSourceIds, String sql) throws Exception {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> allData = new ArrayList<>();

        for (Long dataSourceId : dataSourceIds) {
            com.data.assistant.model.DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                    .orElseThrow(() -> new RuntimeException("DataSource not found: " + dataSourceId));

            try (Connection connection = jdbcDataSource.getConnection();
                 Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("_dataSourceId", dataSourceId);
                    row.put("_dataSourceName", dataSource.getName());
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnLabel(i), rs.getObject(i));
                    }
                    allData.add(row);
                }
            }
        }

        result.put("data", allData);
        result.put("totalCount", allData.size());
        result.put("dataSourceCount", dataSourceIds.size());

        return result;
    }

    public Map<String, Object> joinAcrossDataSources(Long leftDsId, String leftTable, 
                                                      Long rightDsId, String rightTable,
                                                      String joinColumn) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        // 从左数据源获取数据
        List<Map<String, Object>> leftData = fetchData(leftDsId, leftTable);
        
        // 从右数据源获取数据
        List<Map<String, Object>> rightData = fetchData(rightDsId, rightTable);
        
        // 执行内存JOIN
        List<Map<String, Object>> joinedData = new ArrayList<>();
        
        // 构建右表索引
        Map<Object, List<Map<String, Object>>> rightIndex = new HashMap<>();
        for (Map<String, Object> rightRow : rightData) {
            Object key = rightRow.get(joinColumn);
            if (key != null) {
                rightIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(rightRow);
            }
        }
        
        // JOIN操作
        for (Map<String, Object> leftRow : leftData) {
            Object key = leftRow.get(joinColumn);
            if (key != null && rightIndex.containsKey(key)) {
                for (Map<String, Object> rightRow : rightIndex.get(key)) {
                    Map<String, Object> joinedRow = new LinkedHashMap<>();
                    leftRow.forEach((k, v) -> joinedRow.put("left." + k, v));
                    rightRow.forEach((k, v) -> joinedRow.put("right." + k, v));
                    joinedData.add(joinedRow);
                }
            }
        }
        
        result.put("data", joinedData);
        result.put("leftCount", leftData.size());
        result.put("rightCount", rightData.size());
        result.put("joinedCount", joinedData.size());
        
        return result;
    }

    private List<Map<String, Object>> fetchData(Long dataSourceId, String tableName) throws Exception {
        List<Map<String, Object>> data = new ArrayList<>();
        com.data.assistant.model.DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new RuntimeException("DataSource not found"));

        String sql = "SELECT * FROM " + tableName + " LIMIT 10000";
        
        try (Connection connection = jdbcDataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnLabel(i), rs.getObject(i));
                }
                data.add(row);
            }
        }
        
        return data;
    }

    public List<Map<String, Object>> aggregateAcrossDataSources(List<Long> dataSourceIds, 
                                                                 String tableName,
                                                                 String aggColumn,
                                                                 String aggFunction) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (Long dataSourceId : dataSourceIds) {
            com.data.assistant.model.DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                    .orElseThrow(() -> new RuntimeException("DataSource not found"));

            String sql = String.format("SELECT %s(%s) as result FROM %s", 
                aggFunction, aggColumn, tableName);
            
            try (Connection connection = jdbcDataSource.getConnection();
                 Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                if (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("dataSourceId", dataSourceId);
                    row.put("dataSourceName", dataSource.getName());
                    row.put("result", rs.getObject("result"));
                    results.add(row);
                }
            }
        }
        
        return results;
    }
}
