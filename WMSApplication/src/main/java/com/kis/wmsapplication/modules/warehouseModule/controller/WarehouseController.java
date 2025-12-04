package com.kis.wmsapplication.modules.warehouseModule.controller;



import com.kis.wmsapplication.modules.warehouseModule.dto.CreateLevelRequest;
import com.kis.wmsapplication.modules.warehouseModule.dto.HierarchyNodeDto;
import com.kis.wmsapplication.modules.warehouseModule.service.TopologyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final TopologyService topologyService;


    @PostMapping("/topology")
    public ResponseEntity<UUID> addTopologyLevel(@RequestBody @Valid CreateLevelRequest request) {
        return ResponseEntity.ok(topologyService.createLevel(request));
    }


    @GetMapping("/{id}/topology")
    public ResponseEntity<List<HierarchyNodeDto>> getWarehouseTree(@PathVariable UUID id) {
        return ResponseEntity.ok(topologyService.getWarehouseTopology(id));
    }
}