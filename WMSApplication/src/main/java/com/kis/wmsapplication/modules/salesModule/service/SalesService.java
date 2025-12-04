package com.kis.wmsapplication.modules.salesModule.service;


import com.kis.wmsapplication.modules.catalogModule.model.Product;
import com.kis.wmsapplication.modules.catalogModule.repository.ProductRepository;
import com.kis.wmsapplication.modules.inventoryModule.service.InventoryService;
import com.kis.wmsapplication.modules.salesModule.dto.CreateSalesOrderRequest;
import com.kis.wmsapplication.modules.salesModule.model.Customer;
import com.kis.wmsapplication.modules.salesModule.model.OutgoingOrder;
import com.kis.wmsapplication.modules.salesModule.model.OutgoingOrderItem;
import com.kis.wmsapplication.modules.salesModule.enums.SalesStatus;
import com.kis.wmsapplication.modules.salesModule.repository.CustomerRepository;
import com.kis.wmsapplication.modules.salesModule.repository.OutgoingOrderRepository;
import com.kis.wmsapplication.modules.userModule.Exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final OutgoingOrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    @Transactional
    public Long createOrder(CreateSalesOrderRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Клиент не найден"));

        OutgoingOrder order = OutgoingOrder.builder()
                .customer(customer)
                .status(SalesStatus.NEW)
                .totalPrice(BigDecimal.ZERO)
                .build();

        BigDecimal totalOrderPrice = BigDecimal.ZERO;

        for (var itemDto : request.items()) {
            Product product = productRepository.findById(itemDto.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Товар не найден"));

            // 1. Пытаемся зарезервировать товар на складе
            // inventoryService.reserveStock возвращает сколько УДАЛОСЬ зарезервировать
            BigDecimal reservedQty = inventoryService.reserveStock(product.getId(), itemDto.quantity());

            OutgoingOrderItem item = OutgoingOrderItem.builder()
                    .product(product)
                    .quantity(itemDto.quantity()) // Хотим купить
                    .reservedQuantity(reservedQty) // Реально отложили
                    .unitPrice(product.getPrice())
                    .build();

            order.addItem(item);

            totalOrderPrice = totalOrderPrice.add(product.getPrice().multiply(itemDto.quantity()));
        }

        order.setTotalPrice(totalOrderPrice);


        order.setStatus(SalesStatus.RESERVED);

        return orderRepository.save(order).getId();
    }
}