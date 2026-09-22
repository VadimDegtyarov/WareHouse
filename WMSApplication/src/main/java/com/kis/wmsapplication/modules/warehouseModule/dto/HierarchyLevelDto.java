package com.kis.wmsapplication.modules.warehouseModule.dto;


import java.math.BigDecimal;

public record HierarchyLevelDto(
        Long id,
        Long parentId,
        String categoryName,
        String code,
        String name,
        BigDecimal capacity
) {}