package com.kis.wmsapplication.modules.catalogModule.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,


        BigDecimal currentStock,
        BigDecimal reservedStock,
        BigDecimal availableStock,


        BigDecimal minStock,
        BigDecimal maxStock,
        BigDecimal reorderPoint,
        BigDecimal eoq,
        Boolean active,

        Long supplierId,
        String supplierName,

        Set<CategorySummaryDto> categories,

        Long unitId,
        String unitCode,
        String unitDescription
) {
    public record CategorySummaryDto(Long id, String name) {}
}