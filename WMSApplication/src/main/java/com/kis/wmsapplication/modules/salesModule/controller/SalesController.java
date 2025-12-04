package com.kis.wmsapplication.modules.salesModule.controller;

import com.kis.wmsapplication.modules.salesModule.dto.CreateSalesOrderRequest;
import com.kis.wmsapplication.modules.salesModule.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales/orders")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    @PostMapping
    public ResponseEntity<Long> createSalesOrder(@RequestBody @Valid CreateSalesOrderRequest request) {
        return ResponseEntity.ok(salesService.createOrder(request));
    }
}