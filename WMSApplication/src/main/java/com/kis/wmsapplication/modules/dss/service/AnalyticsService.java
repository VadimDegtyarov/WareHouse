package com.kis.wmsapplication.modules.dss.service;

import com.kis.wmsapplication.modules.dss.analytics.AdvancedAnalyticsRepository;
import com.kis.wmsapplication.modules.dss.analytics.StockAnalyticsRepository;
import com.kis.wmsapplication.modules.dss.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AdvancedAnalyticsRepository analyticsRepository;
    private final StockAnalyticsRepository stockAnalyticsRepository;

    public DashboardMetricsDto getDashboardMetrics() {
        List<StockReportDto> stockReport = analyticsRepository.getStockReport();
        
        long totalProducts = stockReport.size();
        long activeProducts = totalProducts;
        long lowStockProducts = stockReport.stream()
                .filter(s -> "LOW".equals(s.status()) || "OUT".equals(s.status()))
                .count();
        long outOfStockProducts = stockReport.stream()
                .filter(s -> "OUT".equals(s.status()))
                .count();
        
        BigDecimal totalStockValue = stockReport.stream()
                .map(StockReportDto::stockValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalReservedValue = stockReport.stream()
                .map(s -> {
                    // Получаем цену из отчета или используем дефолтную
                    BigDecimal price = s.stockValue().compareTo(BigDecimal.ZERO) > 0 && s.currentStock().compareTo(BigDecimal.ZERO) > 0
                            ? s.stockValue().divide(s.currentStock(), 2, java.math.RoundingMode.HALF_UP)
                            : BigDecimal.ONE;
                    return s.reservedStock().multiply(price);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Топ товары по продажам (из реальных данных за последние 30 дней)
        Instant thirtyDaysAgo = Instant.now().minusSeconds(30 * 24 * 60 * 60);
        SalesAnalyticsDto recentSales = analyticsRepository.getSalesAnalytics(thirtyDaysAgo, Instant.now());
        List<DashboardMetricsDto.TopProductDto> topSellingProducts = recentSales != null && recentSales.topProducts() != null
                ? recentSales.topProducts().stream()
                        .limit(10)
                        .map(p -> new DashboardMetricsDto.TopProductDto(
                                p.productId(),
                                p.sku(),
                                p.name(),
                                p.totalRevenue(),
                                p.totalQuantity()
                        ))
                        .collect(Collectors.toList())
                : new ArrayList<>();
        
        // Топ товары по стоимости запасов
        List<DashboardMetricsDto.TopProductDto> topStockValueProducts = stockReport.stream()
                .sorted((a, b) -> b.stockValue().compareTo(a.stockValue()))
                .limit(10)
                .map(s -> new DashboardMetricsDto.TopProductDto(
                        s.productId(),
                        s.sku(),
                        s.name(),
                        s.stockValue(),
                        s.currentStock()
                ))
                .collect(Collectors.toList());
        
        // Алерты о перезаказе
        List<ReorderCandidateDto> reorderCandidates = stockAnalyticsRepository.findProductsBelowReorderPoint();
        List<DashboardMetricsDto.ReorderAlertDto> reorderAlerts = reorderCandidates.stream()
                .limit(10)
                .map(c -> new DashboardMetricsDto.ReorderAlertDto(
                        c.productId(),
                        c.sku(),
                        "", // name будет получен отдельно
                        c.currentAvailable(),
                        c.reorderPoint(),
                        c.deficit()
                ))
                .collect(Collectors.toList());
        
        // Тренды продаж (за последние 30 дней)
        Instant thirtyDaysAgoForTrend = Instant.now().minusSeconds(30 * 24 * 60 * 60);
        SalesAnalyticsDto salesDataForTrend = analyticsRepository.getSalesAnalytics(thirtyDaysAgoForTrend, Instant.now());
        List<DashboardMetricsDto.DailyMetricDto> salesTrend = salesDataForTrend != null && salesDataForTrend.dailySales() != null
                ? salesDataForTrend.dailySales().stream()
                        .map(d -> new DashboardMetricsDto.DailyMetricDto(
                                d.date(),
                                d.dailyRevenue()
                        ))
                        .collect(Collectors.toList())
                : new ArrayList<>();
        
        // Тренды запасов (упрощенная версия - можно расширить с историей)
        List<DashboardMetricsDto.DailyMetricDto> stockTrend = new ArrayList<>();
        
        return new DashboardMetricsDto(
                totalProducts,
                activeProducts,
                lowStockProducts,
                outOfStockProducts,
                totalStockValue,
                totalReservedValue,
                getOrdersCreatedToday(), 
                getOrdersReceivedToday(), 
                getSalesOrdersToday(), 
                getSalesRevenueToday(), 
                getProcurementCostToday(),
                topSellingProducts,
                topStockValueProducts,
                reorderAlerts,
                salesTrend,
                stockTrend
        );
    }

    public List<StockReportDto> getStockReport() {
        return analyticsRepository.getStockReport();
    }

    public List<ABCXYZAnalysisDto> getABCXYZAnalysis(int days) {
        return analyticsRepository.getABCXYZAnalysis(days);
    }

    public List<MovementHistoryDto> getMovementHistory(Long productId, Instant fromDate, Instant toDate) {
        return analyticsRepository.getMovementHistory(productId, fromDate, toDate);
    }

    public List<SupplierPerformanceDto> getSupplierPerformance(int days) {
        return analyticsRepository.getSupplierPerformance(days);
    }

    public SalesAnalyticsDto getSalesAnalytics(Instant fromDate, Instant toDate) {
        return analyticsRepository.getSalesAnalytics(fromDate, toDate);
    }

    private BigDecimal getProductPrice(Long productId) {
        // Упрощенная версия - в реальности нужно получать из репозитория
        return BigDecimal.ONE;
    }

    private long getOrdersCreatedToday() {
        try {
            Instant today = Instant.now().minusSeconds(24 * 60 * 60);
            SalesAnalyticsDto sales = analyticsRepository.getSalesAnalytics(today, Instant.now());
            return sales != null ? sales.totalOrders() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private long getOrdersReceivedToday() {
        // Можно расширить с реальными данными из incoming_order
        return 0;
    }

    private long getSalesOrdersToday() {
        try {
            Instant today = Instant.now().minusSeconds(24 * 60 * 60);
            SalesAnalyticsDto sales = analyticsRepository.getSalesAnalytics(today, Instant.now());
            return sales != null ? sales.totalOrders() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private BigDecimal getSalesRevenueToday() {
        try {
            Instant today = Instant.now().minusSeconds(24 * 60 * 60);
            SalesAnalyticsDto sales = analyticsRepository.getSalesAnalytics(today, Instant.now());
            return sales != null && sales.totalRevenue() != null ? sales.totalRevenue() : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getProcurementCostToday() {
        // Можно расширить с реальными данными из incoming_order
        return BigDecimal.ZERO;
    }
}

