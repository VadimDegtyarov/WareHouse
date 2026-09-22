package com.kis.wmsapplication.modules.inventoryModule.controller;

import com.kis.wmsapplication.modules.inventoryModule.dto.StocktakeRequest;
import com.kis.wmsapplication.modules.inventoryModule.dto.StocktakeResult;
import com.kis.wmsapplication.modules.inventoryModule.service.StocktakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/stocktake")
@RequiredArgsConstructor
public class StocktakeController {

    private final StocktakeService stocktakeService;


    @GetMapping("/{locationId}/sheet")
    public ResponseEntity<List<StocktakeResult.StocktakeLine>> getSheet(@PathVariable Long locationId) {
        return ResponseEntity.ok(stocktakeService.getLocationStock(locationId));
    }


    @PostMapping
    public ResponseEntity<StocktakeResult> perform(@Valid @RequestBody StocktakeRequest request) {
        return ResponseEntity.ok(stocktakeService.perform(request));
    }
}
