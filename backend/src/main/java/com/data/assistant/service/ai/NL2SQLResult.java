package com.data.assistant.service.ai;

import lombok.Data;
import java.util.List;

/**
 * NL2SQL 结果
 */
@Data
public class NL2SQLResult {
    
    /**
     * 生成的 SQL
     */
    private String sql;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 查询意图类型
     */
    private QueryIntent intent;
    
    /**
     * 涉及的表
     */
    private List<String> involvedTables;
    
    /**
     * 查询描述（AI 对查询的解释）
     */
    private String description;
    
    /**
     * 建议的图表类型（如果是分析型查询）
     */
    private SuggestedChart suggestedChart;
    
    public enum QueryIntent {
        QUERY,      // 查询型
        ANALYSIS,   // 分析型
        REPORT,     // 报表型
        UNKNOWN     // 未知
    }
    
    public enum SuggestedChart {
        TABLE,      // 表格
        LINE,       // 折线图
        BAR,        // 柱状图
        PIE,        // 饼图
        SCATTER,    // 散点图
        NONE        // 无
    }
    
    public static NL2SQLResult success(String sql, QueryIntent intent) {
        NL2SQLResult result = new NL2SQLResult();
        result.setSuccess(true);
        result.setSql(sql);
        result.setIntent(intent);
        return result;
    }
    
    public static NL2SQLResult failure(String errorMessage) {
        NL2SQLResult result = new NL2SQLResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setIntent(QueryIntent.UNKNOWN);
        return result;
    }
}
