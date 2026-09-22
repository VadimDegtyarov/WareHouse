package com.kis.wmsapplication.modules.inventoryModule.dto;

import java.math.BigDecimal;
import java.util.List;


public record StockSummaryDto(
        Long productId,
        String productSku,
        String productName,
        
        // Агрегированные данные по всем локациям
        BigDecimal totalQuantity,      // Общее количество на всех локациях
        BigDecimal totalReserved,      // Общее зарезервированное количество
        BigDecimal availableQuantity,  // Доступно для продажи (totalQuantity - totalReserved)
        
        // Параметры управления запасами
        BigDecimal minStock,
        BigDecimal maxStock,
        BigDecimal reorderPoint,
        BigDecimal eoq,
        
        // Расчетные поля
        StockStatus status,           // Статус остатка
        BigDecimal deficit,           // Дефицит (если есть)
        BigDecimal excess,            // Избыток (если есть)
        BigDecimal daysOfStock,       // Дней запаса (на основе средних продаж)
        
        // Детализация по локациям
        List<LocationStockDto> locationStocks
) {

    public enum StockStatus {
        OUT_OF_STOCK,      // Нет в наличии (availableQuantity <= 0)
        CRITICAL,          // Критически низкий (availableQuantity < minStock)
        LOW,               // Низкий остаток (availableQuantity < reorderPoint)
        NORMAL,            // Норма
        EXCESS             // Избыток (availableQuantity > maxStock)
    }
    

    public record LocationStockDto(
            Long locationId,
            String locationCode,
            String locationName,
            String locationType,
            BigDecimal quantity,
            BigDecimal reserved,
            BigDecimal available
    ) {}
    

    public static StockSummaryDto create(
            Long productId, String sku, String name,
            BigDecimal totalQty, BigDecimal totalRes,
            BigDecimal minStock, BigDecimal maxStock, BigDecimal reorderPoint, BigDecimal eoq,
            BigDecimal avgDailySales,
            List<LocationStockDto> locations
    ) {
        BigDecimal available = totalQty.subtract(totalRes);
        
        StockStatus status;
        BigDecimal deficit = BigDecimal.ZERO;
        BigDecimal excess = BigDecimal.ZERO;
        
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            status = StockStatus.OUT_OF_STOCK;
            if (reorderPoint != null) {
                deficit = reorderPoint.subtract(available);
            }
        } else if (minStock != null && available.compareTo(minStock) < 0) {
            status = StockStatus.CRITICAL;
            deficit = minStock.subtract(available);
        } else if (reorderPoint != null && available.compareTo(reorderPoint) < 0) {
            status = StockStatus.LOW;
            deficit = reorderPoint.subtract(available);
        } else if (maxStock != null && available.compareTo(maxStock) > 0) {
            status = StockStatus.EXCESS;
            excess = available.subtract(maxStock);
        } else {
            status = StockStatus.NORMAL;
        }
        
        BigDecimal daysOfStock = null;
        if (avgDailySales != null && avgDailySales.compareTo(BigDecimal.ZERO) > 0) {
            daysOfStock = available.divide(avgDailySales, 1, java.math.RoundingMode.HALF_UP);
        }
        
        return new StockSummaryDto(
                productId, sku, name,
                totalQty, totalRes, available,
                minStock, maxStock, reorderPoint, eoq,
                status, deficit, excess, daysOfStock,
                locations
        );
    }
}

