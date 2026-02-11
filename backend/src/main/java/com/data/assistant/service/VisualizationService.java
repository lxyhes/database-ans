package com.data.assistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 数据可视化服务
 * 生成图表数据用于前端渲染
 */
@Service
public class VisualizationService {
    private static final Logger logger = LoggerFactory.getLogger(VisualizationService.class);

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public VisualizationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 获取销售趋势数据（折线图）
     */
    public Map<String, Object> getSalesTrendData() {
        try {
            String sql = """
                SELECT sale_date as date, SUM(amount) as total
                FROM sales
                GROUP BY sale_date
                ORDER BY sale_date
                """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

            List<String> dates = new ArrayList<>();
            List<Double> totals = new ArrayList<>();

            for (Map<String, Object> row : results) {
                dates.add(row.get("DATE").toString());
                totals.add(((Number) row.get("TOTAL")).doubleValue());
            }

            return Map.of(
                    "type", "line",
                    "title", "销售趋势",
                    "categories", dates,
                    "series", List.of(Map.of("name", "销售额", "data", totals))
            );
        } catch (Exception e) {
            logger.error("Failed to get sales trend data", e);
            return Map.of("type", "line", "title", "销售趋势", "categories", List.of(), "series", List.of());
        }
    }

    /**
     * 获取地区销售对比数据（柱状图）
     */
    public Map<String, Object> getRegionSalesData() {
        try {
            String sql = """
                SELECT region, SUM(amount) as total
                FROM sales
                GROUP BY region
                ORDER BY total DESC
                """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

            List<String> regions = new ArrayList<>();
            List<Double> totals = new ArrayList<>();

            for (Map<String, Object> row : results) {
                regions.add(row.get("REGION").toString());
                totals.add(((Number) row.get("TOTAL")).doubleValue());
            }

            return Map.of(
                    "type", "bar",
                    "title", "各地区销售对比",
                    "categories", regions,
                    "series", List.of(Map.of("name", "销售额", "data", totals))
            );
        } catch (Exception e) {
            logger.error("Failed to get region sales data", e);
            return Map.of("type", "bar", "title", "各地区销售对比", "categories", List.of(), "series", List.of());
        }
    }

    /**
     * 获取产品销售占比数据（饼图）
     */
    public Map<String, Object> getProductSalesData() {
        try {
            String sql = """
                SELECT p.name, SUM(s.amount) as total
                FROM sales s
                JOIN products p ON s.product_id = p.id
                GROUP BY p.name
                ORDER BY total DESC
                """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

            List<String> productNames = new ArrayList<>();
            List<Double> totals = new ArrayList<>();

            for (Map<String, Object> row : results) {
                productNames.add(row.get("NAME").toString());
                totals.add(((Number) row.get("TOTAL")).doubleValue());
            }

            return Map.of(
                    "type", "pie",
                    "title", "产品销售占比",
                    "categories", productNames,
                    "data", totals
            );
        } catch (Exception e) {
            logger.error("Failed to get product sales data", e);
            return Map.of("type", "pie", "title", "产品销售占比", "categories", List.of(), "data", List.of());
        }
    }

    /**
     * 获取客户购买频次数据（柱状图）
     */
    public Map<String, Object> getCustomerPurchaseData() {
        try {
            String sql = """
                SELECT c.name, COUNT(*) as purchase_count
                FROM sales s
                JOIN customers c ON s.customer_id = c.id
                GROUP BY c.name
                ORDER BY purchase_count DESC
                """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

            List<String> customerNames = new ArrayList<>();
            List<Long> purchaseCounts = new ArrayList<>();

            for (Map<String, Object> row : results) {
                customerNames.add(row.get("NAME").toString());
                purchaseCounts.add(((Number) row.get("PURCHASE_COUNT")).longValue());
            }

            return Map.of(
                    "type", "bar",
                    "title", "客户购买频次",
                    "categories", customerNames,
                    "series", List.of(Map.of("name", "购买次数", "data", purchaseCounts))
            );
        } catch (Exception e) {
            logger.error("Failed to get customer purchase data", e);
            return Map.of("type", "bar", "title", "客户购买频次", "categories", List.of(), "series", List.of());
        }
    }

    /**
     * 获取关键指标摘要
     */
    public Map<String, Object> getKeyMetrics() {
        try {
            // 总销售额
            String totalSalesSql = "SELECT COALESCE(SUM(amount), 0) as total FROM sales";
            Double totalSales = jdbcTemplate.queryForObject(totalSalesSql, Double.class);

            // 订单数量
            String orderCountSql = "SELECT COUNT(*) as count FROM sales";
            Long orderCount = jdbcTemplate.queryForObject(orderCountSql, Long.class);

            // 客户数量
            String customerCountSql = "SELECT COUNT(*) as count FROM customers";
            Long customerCount = jdbcTemplate.queryForObject(customerCountSql, Long.class);

            // 产品数量
            String productCountSql = "SELECT COUNT(*) as count FROM products";
            Long productCount = jdbcTemplate.queryForObject(productCountSql, Long.class);

            return Map.of(
                    "totalSales", totalSales != null ? totalSales : 0.0,
                    "orderCount", orderCount != null ? orderCount : 0L,
                    "customerCount", customerCount != null ? customerCount : 0L,
                    "productCount", productCount != null ? productCount : 0L
            );
        } catch (Exception e) {
            logger.error("Failed to get key metrics", e);
            return Map.of(
                    "totalSales", 0.0,
                    "orderCount", 0L,
                    "customerCount", 0L,
                    "productCount", 0L
            );
        }
    }
}