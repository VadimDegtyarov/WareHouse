package com.kis.wmsapplication.modules.catalogModule.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;


public record ProductRequest(
        @NotBlank(message = "Артикул обязателен")
        @Size(min = 1, max = 100, message = "Артикул должен быть от 1 до 100 символов")
        @Pattern(regexp = "^[A-Za-z0-9\\-_]+$", message = "Артикул может содержать только буквы, цифры, дефис и подчеркивание")
        String sku,

        @NotBlank(message = "Название товара обязательно")
        @Size(min = 2, max = 255, message = "Название должно быть от 2 до 255 символов")
        String name,

        @Size(max = 2000, message = "Описание не может быть длиннее 2000 символов")
        String description,

        @NotNull(message = "Цена обязательна")
        @Positive(message = "Цена должна быть больше нуля")
        @DecimalMax(value = "99999999999999.99", message = "Цена превышает максимальное значение")
        BigDecimal price,

        @PositiveOrZero(message = "Минимальный запас не может быть отрицательным")
        @DecimalMax(value = "999999999.999", message = "Минимальный запас превышает максимальное значение")
        BigDecimal minStock,

        @PositiveOrZero(message = "Максимальный запас не может быть отрицательным")
        @DecimalMax(value = "999999999.999", message = "Максимальный запас превышает максимальное значение")
        BigDecimal maxStock,

        @PositiveOrZero(message = "Точка заказа не может быть отрицательной")
        @DecimalMax(value = "999999999.999", message = "Точка заказа превышает максимальное значение")
        BigDecimal reorderPoint,

        @PositiveOrZero(message = "EOQ не может быть отрицательным")
        @DecimalMax(value = "999999999.999", message = "EOQ превышает максимальное значение")
        BigDecimal eoq,

        @Positive(message = "ID поставщика должен быть положительным")
        Long supplierId,

        @NotNull(message = "Список категорий обязателен")
        @NotEmpty(message = "Товар должен иметь хотя бы одну категорию")
        @Size(max = 20, message = "Товар не может принадлежать более чем 20 категориям")
        List<@NotNull @Positive(message = "ID категории должен быть положительным") Long> categories,

        @NotNull(message = "Единица измерения обязательна")
        @Positive(message = "ID единицы измерения должен быть положительным")
        Long unitId,

        @Positive(message = "ID локации должен быть положительным")
        Long locationId, // Локация на складе, куда будет размещен товар

        @PositiveOrZero(message = "Начальное количество не может быть отрицательным")
        @DecimalMax(value = "999999999.999", message = "Начальное количество превышает максимальное значение")
        BigDecimal initialQuantity // Начальное количество товара в локации
) {

    public ProductRequest {
        if (minStock != null && reorderPoint != null && minStock.compareTo(reorderPoint) > 0) {
            throw new IllegalArgumentException("Минимальный запас не может быть больше точки заказа");
        }
        if (reorderPoint != null && maxStock != null && reorderPoint.compareTo(maxStock) > 0) {
            throw new IllegalArgumentException("Точка заказа не может быть больше максимального запаса");
        }
        if (minStock != null && maxStock != null && minStock.compareTo(maxStock) > 0) {
            throw new IllegalArgumentException("Минимальный запас не может быть больше максимального");
        }
    }
}
