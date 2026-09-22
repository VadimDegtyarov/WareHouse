package com.kis.wmsapplication.modules.salesModule.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OutgoingOrderDto(
        Long id,
        Long customerId,
        String customerName,
        String status,
        Instant createdAt,
        Instant shippedAt,
        BigDecimal totalPrice,
        List<OrderItemDto> items
) {
    public record OrderItemDto(
            Long productId,
            String productSku,
            String productName,
            BigDecimal quantity,
            BigDecimal reservedQuantity,
            BigDecimal unitPrice
    ) {}
}

