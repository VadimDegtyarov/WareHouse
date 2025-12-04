package com.kis.wmsapplication.modules.catalogModule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductRequest(
        @NotBlank(message = "Артикул не может быть пустым")
        String sku,

        @NotBlank(message = "Название не может быть пустым")
        String name,

        String description,

        @NotNull @Positive
        BigDecimal price,

        BigDecimal minStock,
        BigDecimal reorderPoint,
        BigDecimal eoq,

        UUID supplierId,

        @NotNull
        List<UUID> categories,

        @NotNull
        UUID unitId
) {}