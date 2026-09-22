package com.kis.wmsapplication.modules.dss.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record DashboardMetricsDto(
        // Основные метрики
        long totalProducts,
        long activeProducts,
        long lowStockProducts,
        long outOfStockProducts,
        BigDecimal totalStockValue,
        BigDecimal totalReservedValue,
        
        // Метрики за период
        long ordersCreatedToday,
        long ordersReceivedToday,
        long salesOrdersToday,
        BigDecimal salesRevenueToday,
        BigDecimal procurementCostToday,
        
        // Топ товары
        List<TopProductDto> topSellingProducts,
        List<TopProductDto> topStockValueProducts,
        List<ReorderAlertDto> reorderAlerts,
        
        // Тренды
        List<DailyMetricDto> salesTrend,
        List<DailyMetricDto> stockTrend
) {
    public record TopProductDto(
            Long productId,
            String sku,
            String name,
            BigDecimal value,
            BigDecimal quantity
    ) {}
    
    public record ReorderAlertDto(
            Long productId,
            String sku,
            String name,
            BigDecimal currentStock,
            BigDecimal reorderPoint,
            BigDecimal deficit
    ) {}
    
    public record DailyMetricDto(
            Instant date,
            BigDecimal value
    ) {}
}

