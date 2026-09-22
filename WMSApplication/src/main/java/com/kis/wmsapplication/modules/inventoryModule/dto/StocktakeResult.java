package com.kis.wmsapplication.modules.inventoryModule.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Результат инвентаризации: список расхождений и корректирующих движений.
 */
public record StocktakeResult(
        Long locationId,
        String locationCode,
        int totalChecked,
        int totalDiscrepancies,
        BigDecimal totalAdjustment,
        List<StocktakeLine> lines
) {
    public record StocktakeLine(
            Long productId,
            String sku,
            String productName,
            BigDecimal systemQuantity,
            BigDecimal countedQuantity,
            BigDecimal difference,
            String action
    ) {}
}
