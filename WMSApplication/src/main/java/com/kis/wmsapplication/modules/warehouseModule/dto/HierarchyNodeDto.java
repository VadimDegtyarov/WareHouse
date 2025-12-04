package com.kis.wmsapplication.modules.warehouseModule.dto;


import java.util.List;
import java.util.UUID;

public record HierarchyNodeDto(
        UUID id,
        String code,
        String name,
        String type, // Category name
        List<HierarchyNodeDto> children // Вложенные элементы
) {}