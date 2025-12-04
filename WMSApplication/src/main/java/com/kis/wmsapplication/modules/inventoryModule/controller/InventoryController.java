package com.kis.wmsapplication.modules.inventoryModule.controller;

import com.kis.wmsapplication.modules.inventoryModule.dto.StockOperationDto;
import com.kis.wmsapplication.modules.inventoryModule.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/move")
    public ResponseEntity<Void> moveStock(@RequestBody @Valid StockOperationDto request) {
        inventoryService.processOperation(request);
        return ResponseEntity.ok().build();
    }
}