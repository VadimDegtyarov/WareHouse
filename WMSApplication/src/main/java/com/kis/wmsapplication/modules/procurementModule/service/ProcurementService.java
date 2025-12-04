package com.kis.wmsapplication.modules.procurementModule.service;


import com.kis.wmsapplication.modules.catalogModule.model.Product;
import com.kis.wmsapplication.modules.catalogModule.repository.ProductRepository;
import com.kis.wmsapplication.modules.inventoryModule.dto.StockOperationDto;
import com.kis.wmsapplication.modules.inventoryModule.enums.MovementType;
import com.kis.wmsapplication.modules.inventoryModule.service.InventoryService;
import com.kis.wmsapplication.modules.procurementModule.dto.CreateOrderRequest;
import com.kis.wmsapplication.modules.procurementModule.model.IncomingOrder;
import com.kis.wmsapplication.modules.procurementModule.model.IncomingOrderItem;
import com.kis.wmsapplication.modules.procurementModule.model.Supplier;
import com.kis.wmsapplication.modules.procurementModule.enums.PurchaseStatus;
import com.kis.wmsapplication.modules.procurementModule.repository.IncomingOrderRepository;
import com.kis.wmsapplication.modules.procurementModule.repository.SupplierRepository;
import com.kis.wmsapplication.modules.userModule.Exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcurementService {

    private final IncomingOrderRepository orderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    // Интеграция с модулем Inventory!
    private final InventoryService inventoryService;

    @Transactional
    public UUID createOrder(CreateOrderRequest request) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Поставщик не найден"));

        IncomingOrder order = IncomingOrder.builder()
                .supplier(supplier)
                .status(PurchaseStatus.PENDING) // Сначала черновик
                .orderDate(Instant.now())
                // Прогноз прибытия = Сейчас + Lead Time поставщика
                .expectedArrival(Instant.now().plus(supplier.getAvgLeadTimeDays(), ChronoUnit.DAYS))
                .build();

        for (var itemDto : request.items()) {
            Product product = productRepository.findById(itemDto.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Товар не найден"));

            BigDecimal price = itemDto.purchasePrice() != null
                    ? itemDto.purchasePrice()
                    : product.getPrice();
            IncomingOrderItem item = IncomingOrderItem.builder()
                    .product(product)
                    .order(order)
                    .quantity(itemDto.quantity())
                    .purchasePrice(itemDto.purchasePrice() != null ? itemDto.purchasePrice() : product.getPrice())

                    .build();

            order.addItem(item);
        }

        return orderRepository.save(order).getId();
    }

    /**
     * Приемка заказа на склад.
     * @param orderId ID заказа
     * @param targetLocationId В какую зону/ячейку принимаем товар (обычно Зона Приемки)
     */
    @Transactional
    public void receiveOrder(UUID orderId, UUID targetLocationId) {
        IncomingOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ не найден"));

        if (order.getStatus() == PurchaseStatus.RECEIVED) {
            throw new IllegalStateException("Заказ уже принят");
        }

        // 1. Проходим по всем товарам в заказе и добавляем их на склад
        for (IncomingOrderItem item : order.getItems()) {
            StockOperationDto receiptOp = new StockOperationDto(
                    item.getProduct().getId(),
                    null, // fromLocation (null, т.к. извне)
                    targetLocationId, // toLocation
                    item.getQuantity(),
                    MovementType.RECEIPT,
                    "Приемка заказа поставщика №" + order.getId()
            );

            // Вызываем сервис Инвентаризации
            inventoryService.processOperation(receiptOp);
        }

        // 2. Обновляем статус заказа
        order.setStatus(PurchaseStatus.RECEIVED);
        order.setActualArrival(Instant.now());
        orderRepository.save(order);
    }
}