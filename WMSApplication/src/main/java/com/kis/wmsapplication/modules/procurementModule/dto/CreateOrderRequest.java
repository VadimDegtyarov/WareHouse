package com.kis.wmsapplication.modules.procurementModule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO для создания заказа на закупку.
 */
public record CreateOrderRequest(
        @NotNull(message = "ID поставщика обязателен")
        @Positive(message = "ID поставщика должен быть положительным")
        Long supplierId,

        @NotEmpty(message = "Заказ должен содержать хотя бы один товар")
        @Size(max = 100, message = "Заказ не может содержать более 100 позиций")
        @Valid
        List<OrderItemDto> items
) {
    /**
     * Позиция заказа на закупку.
     */
    public record OrderItemDto(
            @NotNull(message = "ID товара обязателен")
            @Positive(message = "ID товара должен быть положительным")
            Long productId,

            @NotNull(message = "Количество обязательно")
            @Positive(message = "Количество должно быть больше нуля")
            @DecimalMax(value = "999999999.999", message = "Количество превышает максимальное значение")
            BigDecimal quantity,

            @PositiveOrZero(message = "Закупочная цена не может быть отрицательной")
            @DecimalMax(value = "99999999999999.99", message = "Цена превышает максимальное значение")
            BigDecimal purchasePrice
    ) {}
}
