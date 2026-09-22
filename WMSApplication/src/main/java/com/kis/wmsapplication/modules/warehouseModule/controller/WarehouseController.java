package com.kis.wmsapplication.modules.warehouseModule.controller;

import com.kis.wmsapplication.modules.warehouseModule.dto.CreateLevelRequest;
import com.kis.wmsapplication.modules.warehouseModule.dto.HierarchyLevelDto;
import com.kis.wmsapplication.modules.warehouseModule.dto.HierarchyNodeDto;
import com.kis.wmsapplication.modules.warehouseModule.dto.UpdateLevelRequest;
import com.kis.wmsapplication.modules.warehouseModule.service.TopologyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
// @PreAuthorize убран, так как Gateway уже проверил права доступа на уровне маршрутов
public class WarehouseController {

    private final TopologyService topologyService;

    // --- Create ---
    @PostMapping("/topology")
    public ResponseEntity<Long> addTopologyLevel(@RequestBody @Valid CreateLevelRequest request) {
        return ResponseEntity.ok(topologyService.createLevel(request));
    }

    // --- Read (Tree) ---
    @GetMapping("/{id}/topology")
    public ResponseEntity<List<HierarchyNodeDto>> getWarehouseTree(@PathVariable Long id) {
        return ResponseEntity.ok(topologyService.getWarehouseTopology(id));
    }

    // --- Read (Single Level) ---
    // Получить данные конкретной ячейки/зоны для редактирования
    @GetMapping("/topology/{levelId}")
    public ResponseEntity<HierarchyLevelDto> getLevelById(@PathVariable Long levelId) {
        return ResponseEntity.ok(topologyService.getLevelById(levelId));
    }

    // --- Update ---
    @PutMapping("/topology/{levelId}")
    public ResponseEntity<HierarchyLevelDto> updateLevel(
            @PathVariable Long levelId,
            @RequestBody @Valid UpdateLevelRequest request) {
        return ResponseEntity.ok(topologyService.updateLevel(levelId, request));
    }

    // --- Delete ---
    @DeleteMapping("/topology/{levelId}")
    public ResponseEntity<Void> deleteLevel(@PathVariable Long levelId) {
        topologyService.deleteLevel(levelId);
        return ResponseEntity.noContent().build();
    }
}