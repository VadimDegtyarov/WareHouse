package com.kis.wmsapplication.modules.procurementModule.dto;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID supplierId,
        @NotEmpty List<OrderItemDto> items
) {
    public record OrderItemDto(
            @NotNull UUID productId,
            @NotNull @Positive BigDecimal quantity,
            BigDecimal purchasePrice
    ) {}
}