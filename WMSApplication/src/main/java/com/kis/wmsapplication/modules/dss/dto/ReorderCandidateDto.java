package com.kis.wmsapplication.modules.dss.dto;



import java.math.BigDecimal;
import java.util.UUID;

public record ReorderCandidateDto(
        Long productId,
        String sku,
        Long supplierId,
        BigDecimal currentAvailable, // (Физический остаток - Резерв)
        BigDecimal reorderPoint,
        BigDecimal eoq,
        BigDecimal maxStock,         // Целевой уровень
        BigDecimal deficit           // На сколько мы ниже точки перезаказа
) {}