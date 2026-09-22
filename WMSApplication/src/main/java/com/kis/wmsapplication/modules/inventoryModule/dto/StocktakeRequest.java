package com.kis.wmsapplication.modules.inventoryModule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/**
 * Запрос на проведение инвентаризации.
 * Содержит фактически подсчитанные количества товаров в локации.
 * Бэкенд сравнит их с учётными остатками и сформирует корректирующие движения.
 */
public record StocktakeRequest(
        @NotNull Long locationId,
        String reference,
        @Valid List<StocktakeItem> items
) {
    public record StocktakeItem(
            @NotNull Long productId,
            @NotNull @PositiveOrZero BigDecimal countedQuantity
    ) {}
}
