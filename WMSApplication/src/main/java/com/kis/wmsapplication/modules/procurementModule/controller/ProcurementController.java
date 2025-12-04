package com.kis.wmsapplication.modules.procurementModule.controller;


import com.kis.wmsapplication.modules.procurementModule.dto.CreateOrderRequest;
import com.kis.wmsapplication.modules.procurementModule.service.ProcurementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/procurement/orders")
@RequiredArgsConstructor
public class ProcurementController {

    private final ProcurementService procurementService;

    // Создать заказ
    @PostMapping
    public ResponseEntity<UUID> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        return ResponseEntity.ok(procurementService.createOrder(request));
    }

    // Принять заказ на склад (Финальная стадия)
    // POST /api/v1/procurement/orders/{id}/receive?locationId=...
    @PostMapping("/{id}/receive")
    public ResponseEntity<Void> receiveOrder(
            @PathVariable UUID id,
            @RequestParam UUID locationId) { // ID зоны приемки

        procurementService.receiveOrder(id, locationId);
        return ResponseEntity.ok().build();
    }
}