package com.kis.wmsapplication.modules.warehouseModule.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * DTO для обновления уровня иерархии склада.
 */
public record UpdateLevelRequest(
        @NotBlank(message = "Код локации обязателен")
        @Size(min = 1, max = 100, message = "Код должен быть от 1 до 100 символов")
        @Pattern(regexp = "^[A-Za-z0-9\\-_]+$", message = "Код может содержать только буквы, цифры, дефис и подчеркивание")
        String code,

        @Size(max = 255, message = "Название не может быть длиннее 255 символов")
        String name,

        @PositiveOrZero(message = "Вместимость не может быть отрицательной")
        @DecimalMax(value = "99999999999.999", message = "Вместимость превышает максимальное значение")
        BigDecimal capacity
) {}
