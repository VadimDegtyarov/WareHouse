package com.kis.wmsapplication.modules.catalogModule.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String description,
        BigDecimal price,


        BigDecimal minStock,
        BigDecimal reorderPoint,
        BigDecimal eoq,
        Boolean active,


        Set<CategorySummaryDto> categories,

        UUID unitId,
        String unitCode,
        String unitDescription
) {
    public record CategorySummaryDto(UUID id, String name) {}
}