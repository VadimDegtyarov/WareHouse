package com.kis.wmsapplication.modules.dss.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SupplierPerformanceDto(
        Long supplierId,
        String supplierName,
        int totalOrders,
        int completedOrders,
        int delayedOrders,
        BigDecimal totalOrderValue,
        BigDecimal averageOrderValue,
        BigDecimal averageLeadTime,
        BigDecimal onTimeDeliveryRate,
        Instant lastOrderDate,
        String rating  // EXCELLENT, GOOD, AVERAGE, POOR
) {}

