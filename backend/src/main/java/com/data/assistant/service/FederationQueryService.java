package com.data.assistant.service;

import com.data.assistant.repository.DataSourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FederationQueryService {

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private DynamicDataSourceService dynamicDataSourceService;

    public Map<String, Object> executeFederationQuery(List<Long> dataSourceIds, String sql) throws Exception {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> allData = new ArrayList<>();

        for (Long dataSourceId : dataSourceIds) {
            com.data.assistant.model.DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                    .orElseThrow(() -> new RuntimeException("DataSource not found: " + dataSourceId));

            try {
                dynamicDataSourceService.switchDataSource(dataSourceId);
                
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
                for (Map<String, Object> row : rows) {
                    Map<String, Object> rowWithSource = new LinkedHashMap<>(row);
                    rowWithSource.put("_dataSourceId", dataSourceId);
                    rowWithSource.put("_dataSourceName", dataSource.getName());
                    allData.add(rowWithSource);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to query dataSource " + dataSourceId + ": " + e.getMessage());
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
        
        List<Map<String, Object>> leftData = fetchData(leftDsId, leftTable);
        
        List<Map<String, Object>> rightData = fetchData(rightDsId, rightTable);
        
        List<Map<String, Object>> joinedData = new ArrayList<>();
        
        Map<Object, List<Map<String, Object>>> rightIndex = new HashMap<>();
        for (Map<String, Object> rightRow : rightData) {
            Object key = rightRow.get(joinColumn);
            if (key != null) {
                rightIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(rightRow);
            }
        }
        
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

        try {
            dynamicDataSourceService.switchDataSource(dataSourceId);
            
            String sql = "SELECT * FROM " + tableName + " LIMIT 10000";
            data = jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch data from dataSource " + dataSourceId + ": " + e.getMessage());
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

            try {
                dynamicDataSourceService.switchDataSource(dataSourceId);
                
                String sql = String.format("SELECT %s(%s) as result FROM %s", 
                    aggFunction, aggColumn, tableName);
                
                Object aggResult = jdbcTemplate.queryForObject(sql, Object.class);
                
                Map<String, Object> row = new HashMap<>();
                row.put("dataSourceId", dataSourceId);
                row.put("dataSourceName", dataSource.getName());
                row.put("result", aggResult);
                results.add(row);
            } catch (Exception e) {
                Map<String, Object> errorRow = new HashMap<>();
                errorRow.put("dataSourceId", dataSourceId);
                errorRow.put("dataSourceName", dataSource.getName());
                errorRow.put("error", e.getMessage());
                results.add(errorRow);
            }
        }
        
        return results;
    }
}
