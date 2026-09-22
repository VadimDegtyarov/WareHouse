package com.kis.wmsapplication.modules.dss.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StockReportDto(
        Long productId,
        String sku,
        String name,
        BigDecimal currentStock,
        BigDecimal reservedStock,
        BigDecimal availableStock,
        BigDecimal minStock,
        BigDecimal reorderPoint,
        BigDecimal maxStock,
        BigDecimal stockValue,
        BigDecimal turnoverRate,
        int daysOfStock,
        String status  // NORMAL, LOW, OUT, EXCESS
) {}

