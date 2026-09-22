package com.kis.wmsapplication.modules.dss.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SalesAnalyticsDto(
        BigDecimal totalRevenue,
        BigDecimal averageOrderValue,
        long totalOrders,
        BigDecimal totalQuantity,
        List<TopProductDto> topProducts,
        List<DailySalesDto> dailySales,
        List<CategorySalesDto> categorySales
) {
    public record TopProductDto(
            Long productId,
            String sku,
            String name,
            BigDecimal totalQuantity,
            BigDecimal totalRevenue
    ) {}
    
    public record DailySalesDto(
            Instant date,
            long ordersCount,
            BigDecimal dailyRevenue
    ) {}
    
    public record CategorySalesDto(
            Long categoryId,
            String categoryName,
            long ordersCount,
            BigDecimal categoryRevenue
    ) {}
}

