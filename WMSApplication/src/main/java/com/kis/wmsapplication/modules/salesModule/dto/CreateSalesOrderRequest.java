package com.kis.wmsapplication.modules.salesModule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO для создания заказа на продажу.
 */
public record CreateSalesOrderRequest(
        @NotNull(message = "ID клиента обязателен")
        @Positive(message = "ID клиента должен быть положительным")
        Long customerId,

        @NotEmpty(message = "Заказ должен содержать хотя бы один товар")
        @Size(max = 100, message = "Заказ не может содержать более 100 позиций")
        @Valid
        List<SalesItemDto> items
) {
    /**
     * Позиция заказа на продажу.
     */
    public record SalesItemDto(
            @NotNull(message = "ID товара обязателен")
            @Positive(message = "ID товара должен быть положительным")
            Long productId,

            @NotNull(message = "Количество обязательно")
            @Positive(message = "Количество должно быть больше нуля")
            @DecimalMax(value = "999999999.999", message = "Количество превышает максимальное значение")
            BigDecimal quantity
    ) {}
}
