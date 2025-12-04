package com.kis.wmsapplication.modules.warehouseModule.service;


import com.kis.wmsapplication.modules.userModule.Exception.ResourceNotFoundException;
import com.kis.wmsapplication.modules.warehouseModule.dto.CreateLevelRequest;
import com.kis.wmsapplication.modules.warehouseModule.dto.HierarchyNodeDto;
import com.kis.wmsapplication.modules.warehouseModule.model.HierarchyLevel;
import com.kis.wmsapplication.modules.warehouseModule.model.HierarchyLevelCategory;
import com.kis.wmsapplication.modules.warehouseModule.model.Warehouse;
import com.kis.wmsapplication.modules.warehouseModule.repository.HierarchyLevelCategoryRepository;
import com.kis.wmsapplication.modules.warehouseModule.repository.HierarchyLevelRepository;
import com.kis.wmsapplication.modules.warehouseModule.repository.WarehouseRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopologyService {

    private final WarehouseRepository warehouseRepository;
    private final HierarchyLevelRepository levelRepository;
    private final HierarchyLevelCategoryRepository categoryRepository;

    @Transactional
    public UUID createLevel(CreateLevelRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Склад не найден"));

        HierarchyLevelCategory category = categoryRepository.findByLevelName(request.categoryName())
                .orElseThrow(() -> new ResourceNotFoundException("Тип уровня не найден: " + request.categoryName()));

        HierarchyLevel level = HierarchyLevel.builder()
                .warehouse(warehouse)
                .code(request.code())
                .name(request.name())
                .capacity(request.capacity())
                .category(category)
                .build();

        if (request.parentId() != null) {
            HierarchyLevel parent = levelRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Родительская ячейка не найдена"));


            if (!parent.getWarehouse().getId().equals(warehouse.getId())) {
                throw new IllegalArgumentException("Родитель находится на другом складе!");
            }

            level.setParent(parent);
        }

        return levelRepository.save(level).getId();
    }

    // Получение дерева склада (для визуализации на фронте)
    @Transactional
    public List<HierarchyNodeDto> getWarehouseTopology(UUID warehouseId) {
        List<HierarchyLevel> roots = levelRepository.findByWarehouseIdAndParentIsNull(warehouseId);

        return roots.stream()
                .map(this::mapToNode)
                .collect(Collectors.toList());
    }

    private HierarchyNodeDto mapToNode(HierarchyLevel level) {
        List<HierarchyNodeDto> childrenDtos = level.getChildren().stream()
                .map(this::mapToNode)
                .collect(Collectors.toList());

        return new HierarchyNodeDto(
                level.getId(),
                level.getCode(),
                level.getName(),
                level.getCategory().getLevelName(),
                childrenDtos
        );
    }
}