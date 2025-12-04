package com.kis.wmsapplication.modules.salesModule.dto;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateSalesOrderRequest(
        @NotNull UUID customerId,
        @NotEmpty List<SalesItemDto> items
) {
    public record SalesItemDto(
            @NotNull UUID productId,
            @NotNull @Positive BigDecimal quantity
    ) {}
}