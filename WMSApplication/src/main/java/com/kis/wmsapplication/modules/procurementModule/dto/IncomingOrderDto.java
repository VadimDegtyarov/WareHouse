package com.kis.wmsapplication.modules.procurementModule.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record IncomingOrderDto(
        Long id,
        Long supplierId,
        String supplierName,
        String status,
        Instant orderDate,
        Instant expectedArrival,
        Instant actualArrival,
        BigDecimal totalValue,
        List<OrderItemDto> items
) {
    public record OrderItemDto(
            Long productId,
            String productSku,
            String productName,
            BigDecimal quantity,
            BigDecimal purchasePrice
    ) {}
}

