package com.kis.wmsapplication.modules.inventoryModule.dto;

import com.kis.wmsapplication.modules.inventoryModule.enums.MovementType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;


public record StockOperationDto(
        @NotNull(message = "ID товара обязателен")
        @Positive(message = "ID товара должен быть положительным")
        Long productId,

        @Positive(message = "ID исходной локации должен быть положительным")
        Long fromLocationId, // Откуда (для TRANSFER, SHIPMENT)

        @Positive(message = "ID целевой локации должен быть положительным")
        Long toLocationId,   // Куда (для RECEIPT, TRANSFER, ADJUSTMENT)

        @NotNull(message = "Количество обязательно")
        @Positive(message = "Количество должно быть больше нуля")
        @DecimalMax(value = "99999999999.999", message = "Количество превышает максимальное значение")
        BigDecimal quantity,

        MovementType type,   // RECEIPT, TRANSFER, SHIPMENT, ADJUSTMENT

        @Size(max = 500, message = "Комментарий не может быть длиннее 500 символов")
        String reference     // Комментарий (№ накладной, причина корректировки)
) {}
