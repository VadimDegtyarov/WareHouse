package com.kis.wmsapplication.modules.warehouseModule.controller;

import com.kis.wmsapplication.modules.warehouseModule.dto.LocationDto;
import com.kis.wmsapplication.modules.warehouseModule.dto.WarehouseDto;
import com.kis.wmsapplication.modules.warehouseModule.model.HierarchyLevel;
import com.kis.wmsapplication.modules.warehouseModule.model.Warehouse;
import com.kis.wmsapplication.modules.warehouseModule.repository.HierarchyLevelRepository;
import com.kis.wmsapplication.modules.warehouseModule.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class WarehouseListController {

    private final WarehouseRepository warehouseRepository;
    private final HierarchyLevelRepository hierarchyLevelRepository;

    @GetMapping("/warehouses")
    public ResponseEntity<List<WarehouseDto>> getAllWarehouses() {
        List<Warehouse> warehouses = warehouseRepository.findAll();
        List<WarehouseDto> warehouseDtos = warehouses.stream()
                .map(warehouse -> new WarehouseDto(
                        warehouse.getId(),
                        warehouse.getCode(),
                        warehouse.getName()
                ))
                .toList();
        return ResponseEntity.ok(warehouseDtos);
    }

    @GetMapping("/locations")
    public ResponseEntity<List<LocationDto>> getAllLocations(
            @RequestParam(required = false) Long warehouseId
    ) {
        List<HierarchyLevel> levels;
        if (warehouseId != null) {
            levels = hierarchyLevelRepository.findByWarehouseId(warehouseId);
        } else {
            levels = hierarchyLevelRepository.findAll();
        }

        List<LocationDto> locationDtos = levels.stream()
                .map(level -> new LocationDto(
                        level.getId(),
                        level.getCode(),
                        level.getName(),
                        level.getCategory().getLevelName()
                ))
                .toList();
        return ResponseEntity.ok(locationDtos);
    }
}

