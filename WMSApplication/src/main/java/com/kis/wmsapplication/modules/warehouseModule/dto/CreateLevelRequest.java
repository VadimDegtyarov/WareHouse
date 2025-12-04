package com.kis.wmsapplication.modules.warehouseModule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateLevelRequest(
        @NotNull UUID warehouseId,
        UUID parentId, // Может быть null (если создаем Зону)
        @NotBlank String categoryName, // "ZONE", "RACK"...
        @NotBlank String code,
        String name,
        BigDecimal capacity
) {}