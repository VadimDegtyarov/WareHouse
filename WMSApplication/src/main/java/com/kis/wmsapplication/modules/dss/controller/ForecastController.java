package com.kis.wmsapplication.modules.dss.controller;

import com.kis.wmsapplication.modules.dss.service.ForecastService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/analytics/forecast")
@RequiredArgsConstructor
public class ForecastController {

    private final ForecastService forecastService;

    /** Прогноз спроса методом скользящего среднего */
    @GetMapping("/demand/{productId}")
    public ResponseEntity<BigDecimal> forecastDemand(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(forecastService.forecastDemand(productId, days));
    }

    /** Средний дневной спрос на товар */
    @GetMapping("/daily-demand/{productId}")
    public ResponseEntity<BigDecimal> getAverageDailyDemand(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "90") int historicalDays
    ) {
        return ResponseEntity.ok(forecastService.getAverageDailyDemand(productId, historicalDays));
    }

    /** Расчёт EOQ (формула Уилсона) */
    @GetMapping("/eoq/{productId}")
    public ResponseEntity<BigDecimal> calculateEOQ(@PathVariable Long productId) {
        return ResponseEntity.ok(forecastService.calculateEOQ(productId));
    }

    /** Расчёт точки перезаказа (ROP) */
    @GetMapping("/rop/{productId}")
    public ResponseEntity<BigDecimal> calculateReorderPoint(@PathVariable Long productId) {
        return ResponseEntity.ok(forecastService.calculateReorderPoint(productId));
    }

    /** Расчёт страхового запаса (Safety Stock) */
    @GetMapping("/safety-stock/{productId}")
    public ResponseEntity<BigDecimal> calculateSafetyStock(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "7") int leadTimeDays
    ) {
        return ResponseEntity.ok(forecastService.calculateSafetyStock(productId, leadTimeDays));
    }

    /** Расчёт всех параметров управления запасами */
    @GetMapping("/parameters/{productId}")
    public ResponseEntity<Map<String, Object>> calculateInventoryParameters(@PathVariable Long productId) {
        return ResponseEntity.ok(forecastService.calculateInventoryParameters(productId));
    }

    /** Оптимальный уровень запасов */
    @GetMapping("/optimal-stock/{productId}")
    public ResponseEntity<BigDecimal> getOptimalStockLevel(
            @PathVariable Long productId,
            @RequestParam BigDecimal currentStock,
            @RequestParam(defaultValue = "7") int leadTimeDays
    ) {
        return ResponseEntity.ok(forecastService.calculateOptimalStockLevel(productId, currentStock, leadTimeDays));
    }

    /** Текстовые рекомендации по оптимизации */
    @GetMapping("/recommendations/{productId}")
    public ResponseEntity<List<String>> getRecommendations(
            @PathVariable Long productId,
            @RequestParam BigDecimal currentStock,
            @RequestParam BigDecimal optimalLevel
    ) {
        return ResponseEntity.ok(forecastService.getOptimizationRecommendations(productId, currentStock, optimalLevel));
    }
}
