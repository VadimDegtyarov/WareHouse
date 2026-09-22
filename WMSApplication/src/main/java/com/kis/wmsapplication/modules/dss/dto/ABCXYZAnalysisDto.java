package com.kis.wmsapplication.modules.dss.dto;

import java.math.BigDecimal;

public record ABCXYZAnalysisDto(
        Long productId,
        String sku,
        String name,
        BigDecimal totalSalesValue,
        BigDecimal totalSalesQuantity,
        BigDecimal averageStock,
        BigDecimal turnoverRate,
        String abcCategory,  // A, B, C
        String xyzCategory,   // X, Y, Z
        String recommendation
) {}

