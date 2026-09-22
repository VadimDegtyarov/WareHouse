package com.kis.wmsapplication.modules.inventoryModule.controller;

import com.kis.wmsapplication.modules.inventoryModule.dto.StockSummaryDto;
import com.kis.wmsapplication.modules.inventoryModule.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;


    @GetMapping
    public ResponseEntity<List<StockSummaryDto>> getAllStockSummaries() {
        return ResponseEntity.ok(stockService.getAllStockSummaries());
    }


    @GetMapping("/{productId}")
    public ResponseEntity<StockSummaryDto> getStockSummary(@PathVariable Long productId) {
        return ResponseEntity.ok(stockService.getStockSummary(productId));
    }


    @GetMapping("/low")
    public ResponseEntity<List<StockSummaryDto>> getLowStockProducts() {
        return ResponseEntity.ok(stockService.getLowStockProducts());
    }


    @GetMapping("/out-of-stock")
    public ResponseEntity<List<StockSummaryDto>> getOutOfStockProducts() {
        return ResponseEntity.ok(stockService.getOutOfStockProducts());
    }


    @GetMapping("/excess")
    public ResponseEntity<List<StockSummaryDto>> getExcessStockProducts() {
        return ResponseEntity.ok(stockService.getExcessStockProducts());
    }


    @GetMapping("/statistics")
    public ResponseEntity<StockService.StockStatistics> getStockStatistics() {
        return ResponseEntity.ok(stockService.getStockStatistics());
    }
}

