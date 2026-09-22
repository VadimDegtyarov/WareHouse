package com.kis.wmsapplication.modules.procurementModule.controller;


import com.kis.wmsapplication.modules.procurementModule.dto.CreateOrderRequest;
import com.kis.wmsapplication.modules.procurementModule.dto.IncomingOrderDto;
import com.kis.wmsapplication.modules.procurementModule.enums.PurchaseStatus;
import com.kis.wmsapplication.modules.procurementModule.model.IncomingOrder;
import com.kis.wmsapplication.modules.procurementModule.repository.IncomingOrderRepository;
import com.kis.wmsapplication.modules.procurementModule.service.ProcurementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/procurement/orders")
@RequiredArgsConstructor
public class ProcurementController {

    private final ProcurementService procurementService;
    private final IncomingOrderRepository orderRepository;

    // Получить список заказов
    @GetMapping
    public ResponseEntity<List<IncomingOrderDto>> getAllOrders(
            @RequestParam(required = false) PurchaseStatus status
    ) {
        List<IncomingOrder> orders;
        if (status != null) {
            orders = orderRepository.findByStatus(status);
        } else {
            orders = orderRepository.findAll();
        }
        
        List<IncomingOrderDto> dtos = orders.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    // Получить заказ по ID
    @GetMapping("/{id}")
    public ResponseEntity<IncomingOrderDto> getOrderById(@PathVariable Long id) {
        IncomingOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
        return ResponseEntity.ok(mapToDto(order));
    }

    // Создать заказ
    @PostMapping
    public ResponseEntity<Long> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        return ResponseEntity.ok(procurementService.createOrder(request));
    }

    // Принять заказ на склад (Финальная стадия)
    // POST /api/v1/procurement/orders/{id}/receive?locationId=...
    @PostMapping("/{id}/receive")
    public ResponseEntity<Void> receiveOrder(
            @PathVariable Long id,
            @RequestParam Long locationId) { // ID зоны приемки

        procurementService.receiveOrder(id, locationId);
        return ResponseEntity.ok().build();
    }

    private IncomingOrderDto mapToDto(IncomingOrder order) {
        BigDecimal totalValue = order.getItems().stream()
                .map(item -> item.getPurchasePrice().multiply(item.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<IncomingOrderDto.OrderItemDto> items = order.getItems().stream()
                .map(item -> new IncomingOrderDto.OrderItemDto(
                        item.getProduct().getId(),
                        item.getProduct().getSku(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPurchasePrice()
                ))
                .collect(Collectors.toList());
        
        return new IncomingOrderDto(
                order.getId(),
                order.getSupplier().getId(),
                order.getSupplier().getName(),
                order.getStatus().name(),
                order.getOrderDate(),
                order.getExpectedArrival(),
                order.getActualArrival(),
                totalValue,
                items
        );
    }
}