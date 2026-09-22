package com.kis.wmsapplication.modules.warehouseModule.dto;


import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record HierarchyNodeDto(
        Long id,
        String code,
        String name,
        String type, // Category name
        BigDecimal totalQuantity, // Общее количество товаров в локации (включая дочерние)
        Integer productCount, // Количество различных товаров в локации
        BigDecimal utilization, // Использование вместимости (0-1 или null если capacity не задана)
        List<StockItemDto> stockItems, // Список товаров в этой локации
        List<HierarchyNodeDto> children // Вложенные элементы
) {
    public record StockItemDto(
            Long productId,
            String productSku,
            String productName,
            BigDecimal quantity,
            BigDecimal reserved
    ) {}
}