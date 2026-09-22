package com.kis.wmsapplication.modules.dss.controller;

import com.kis.wmsapplication.modules.dss.dto.*;
import com.kis.wmsapplication.modules.dss.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardMetricsDto> getDashboardMetrics() {
        return ResponseEntity.ok(analyticsService.getDashboardMetrics());
    }

    @GetMapping("/stock-report")
    public ResponseEntity<List<StockReportDto>> getStockReport() {
        return ResponseEntity.ok(analyticsService.getStockReport());
    }

    @GetMapping("/abcxyz")
    public ResponseEntity<List<ABCXYZAnalysisDto>> getABCXYZAnalysis(
            @RequestParam(defaultValue = "90") int days
    ) {
        return ResponseEntity.ok(analyticsService.getABCXYZAnalysis(days));
    }

    @GetMapping("/movement-history")
    public ResponseEntity<List<MovementHistoryDto>> getMovementHistory(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate
    ) {
        return ResponseEntity.ok(analyticsService.getMovementHistory(productId, fromDate, toDate));
    }

    @GetMapping("/supplier-performance")
    public ResponseEntity<List<SupplierPerformanceDto>> getSupplierPerformance(
            @RequestParam(defaultValue = "90") int days
    ) {
        return ResponseEntity.ok(analyticsService.getSupplierPerformance(days));
    }

    @GetMapping("/sales")
    public ResponseEntity<SalesAnalyticsDto> getSalesAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate
    ) {
        if (fromDate == null) {
            fromDate = Instant.now().minusSeconds(30 * 24 * 60 * 60); // 30 дней назад
        }
        if (toDate == null) {
            toDate = Instant.now();
        }
        return ResponseEntity.ok(analyticsService.getSalesAnalytics(fromDate, toDate));
    }
}

