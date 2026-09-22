package com.kis.wmsapplication.modules.salesModule.controller;

import com.kis.wmsapplication.modules.salesModule.dto.CreateSalesOrderRequest;
import com.kis.wmsapplication.modules.salesModule.dto.OutgoingOrderDto;
import com.kis.wmsapplication.modules.salesModule.enums.SalesStatus;
import com.kis.wmsapplication.modules.salesModule.model.OutgoingOrder;
import com.kis.wmsapplication.modules.salesModule.repository.OutgoingOrderRepository;
import com.kis.wmsapplication.modules.salesModule.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/sales/orders")
@RequiredArgsConstructor
// @PreAuthorize убран, так как Gateway уже проверил права доступа на уровне маршрутов
public class SalesController {

    private final SalesService salesService;
    private final OutgoingOrderRepository orderRepository;

    // Получить список заказов на продажу
    @GetMapping
    public ResponseEntity<List<OutgoingOrderDto>> getAllOrders(
            @RequestParam(required = false) SalesStatus status
    ) {
        List<OutgoingOrder> orders;
        if (status != null) {
            orders = orderRepository.findByStatus(status);
        } else {
            orders = orderRepository.findAll();
        }
        
        List<OutgoingOrderDto> dtos = orders.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    // Получить заказ по ID
    @GetMapping("/{id}")
    public ResponseEntity<OutgoingOrderDto> getOrderById(@PathVariable Long id) {
        OutgoingOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
        return ResponseEntity.ok(mapToDto(order));
    }

    @PostMapping
    public ResponseEntity<Long> createSalesOrder(@RequestBody @Valid CreateSalesOrderRequest request) {
        return ResponseEntity.ok(salesService.createOrder(request));
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<Void> shipOrder(@PathVariable Long id) {
        salesService.shipOrder(id);
        return ResponseEntity.ok().build();
    }

    private OutgoingOrderDto mapToDto(OutgoingOrder order) {
        List<OutgoingOrderDto.OrderItemDto> items = order.getItems().stream()
                .map(item -> new OutgoingOrderDto.OrderItemDto(
                        item.getProduct().getId(),
                        item.getProduct().getSku(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getReservedQuantity(),
                        item.getUnitPrice()
                ))
                .collect(Collectors.toList());
        
        return new OutgoingOrderDto(
                order.getId(),
                order.getCustomer().getId(),
                order.getCustomer().getName(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getShippedAt(),
                order.getTotalPrice(),
                items
        );
    }
}