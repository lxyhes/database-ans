package com.data.assistant.controller;

import com.data.assistant.service.VisualizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据可视化控制器
 */
@RestController
@RequestMapping("/api/visualization")
@CrossOrigin(origins = "*")
public class VisualizationController {

    @Autowired
    private VisualizationService visualizationService;

    /**
     * 获取所有图表数据
     */
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardData() {
        return Map.of(
                "keyMetrics", visualizationService.getKeyMetrics(),
                "salesTrend", visualizationService.getSalesTrendData(),
                "regionSales", visualizationService.getRegionSalesData(),
                "productSales", visualizationService.getProductSalesData(),
                "customerPurchase", visualizationService.getCustomerPurchaseData()
        );
    }

    /**
     * 获取关键指标
     */
    @GetMapping("/metrics")
    public Map<String, Object> getKeyMetrics() {
        return visualizationService.getKeyMetrics();
    }

    /**
     * 获取销售趋势
     */
    @GetMapping("/sales-trend")
    public Map<String, Object> getSalesTrend() {
        return visualizationService.getSalesTrendData();
    }

    /**
     * 获取地区销售
     */
    @GetMapping("/region-sales")
    public Map<String, Object> getRegionSales() {
        return visualizationService.getRegionSalesData();
    }

    /**
     * 获取产品销售
     */
    @GetMapping("/product-sales")
    public Map<String, Object> getProductSales() {
        return visualizationService.getProductSalesData();
    }

    /**
     * 获取客户购买数据
     */
    @GetMapping("/customer-purchase")
    public Map<String, Object> getCustomerPurchase() {
        return visualizationService.getCustomerPurchaseData();
    }
}