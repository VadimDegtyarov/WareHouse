package com.kis.wmsapplication.modules.salesModule.service;


import com.kis.wmsapplication.modules.catalogModule.model.Product;
import com.kis.wmsapplication.modules.catalogModule.repository.ProductRepository;
import com.kis.wmsapplication.modules.inventoryModule.model.ProductLocationStock;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Сервис управления продажами.
 * Обрабатывает заказы на продажу, резервирование и отгрузку товаров.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SalesService {

    private final OutgoingOrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    /**
     * Создание нового заказа на продажу.
     * 
     * @param request данные заказа
     * @return ID созданного заказа
     * @throws ResourceNotFoundException если клиент не найден
     * @throws IllegalArgumentException если недостаточно товара или некорректные данные
     */
    @Transactional
    public Long createOrder(CreateSalesOrderRequest request) {
        // Находим клиента
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Клиент с ID " + request.customerId() + " не найден. " +
                        "Создайте клиента в разделе 'Клиенты' перед созданием заказа."
                ));

        // Валидируем, что есть товары в заказе
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Заказ должен содержать хотя бы один товар");
        }

        OutgoingOrder order = OutgoingOrder.builder()
                .customer(customer)
                .status(SalesStatus.NEW)
                .totalPrice(BigDecimal.ZERO)
                .build();

        BigDecimal totalOrderPrice = BigDecimal.ZERO;

        for (var itemDto : request.items()) {
            Product product = productRepository.findById(itemDto.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Товар с ID " + itemDto.productId() + " не найден"
                    ));

            // Проверка на отрицательное или нулевое количество
            if (itemDto.quantity() == null || itemDto.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Количество товара '" + product.getName() + "' должно быть больше нуля"
                );
            }

            // Пытаемся зарезервировать товар на складе
            BigDecimal reservedQty = inventoryService.reserveStock(product.getId(), itemDto.quantity());

            if (reservedQty.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException(
                        "Недостаточно товара на складе: " + product.getSku() + " (" + product.getName() + "). " +
                        "Запрошено: " + itemDto.quantity() + ", доступно: 0"
                );
            }

            // Предупреждаем если зарезервировано меньше чем запрошено
            if (reservedQty.compareTo(itemDto.quantity()) < 0) {
                log.warn("Частичное резервирование для товара {}: запрошено {}, зарезервировано {}",
                        product.getSku(), itemDto.quantity(), reservedQty);
            }

            OutgoingOrderItem item = OutgoingOrderItem.builder()
                    .product(product)
                    .quantity(itemDto.quantity())
                    .reservedQuantity(reservedQty)
                    .unitPrice(product.getPrice())
                    .build();

            order.addItem(item);

            totalOrderPrice = totalOrderPrice.add(product.getPrice().multiply(itemDto.quantity()));
        }

        order.setTotalPrice(totalOrderPrice);
        order.setStatus(SalesStatus.RESERVED);

        OutgoingOrder savedOrder = orderRepository.save(order);
        
        log.info("Создан заказ на продажу #{} для клиента '{}' на сумму {}",
                savedOrder.getId(), customer.getName(), totalOrderPrice);

        return savedOrder.getId();
    }

    /**
     * Отгрузка заказа - списание товара со склада.
     * 
     * @param orderId ID заказа
     * @throws ResourceNotFoundException если заказ не найден
     * @throws IllegalStateException если заказ в неправильном статусе
     */
    @Transactional
    public void shipOrder(Long orderId) {
        OutgoingOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ с ID " + orderId + " не найден"));

        // Проверяем статус заказа
        if (order.getStatus() != SalesStatus.RESERVED && order.getStatus() != SalesStatus.PICKING) {
            throw new IllegalStateException(
                    "Заказ #" + orderId + " должен быть в статусе RESERVED или PICKING для отгрузки. " +
                    "Текущий статус: " + order.getStatus()
            );
        }

        // Списываем товар со склада
        for (OutgoingOrderItem item : order.getItems()) {
            // Находим все места, где зарезервирован товар
            List<ProductLocationStock> stocks = inventoryService.findStocksByProduct(item.getProduct().getId());
            
            BigDecimal remainingToShip = item.getReservedQuantity();
            
            for (ProductLocationStock stock : stocks) {
                if (remainingToShip.compareTo(BigDecimal.ZERO) <= 0) break;
                
                BigDecimal reservedInStock = stock.getReserved();
                if (reservedInStock.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal toShip = reservedInStock.min(remainingToShip);
                    
                    // Уменьшаем количество и резерв
                    stock.setQuantity(stock.getQuantity().subtract(toShip));
                    stock.setReserved(stock.getReserved().subtract(toShip));
                    
                    // Проверка на отрицательное значение
                    if (stock.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalStateException(
                                "Отрицательное количество товара '" + item.getProduct().getSku() + 
                                "' после списания в локации " + stock.getLocation().getCode()
                        );
                    }
                    if (stock.getReserved().compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalStateException(
                                "Отрицательный резерв товара '" + item.getProduct().getSku() + 
                                "' после списания в локации " + stock.getLocation().getCode()
                        );
                    }
                    
                    inventoryService.saveStock(stock);
                    
                    // Создаем движение SHIPMENT
                    inventoryService.createMovement(
                            item.getProduct(),
                            stock.getLocation(),
                            null,
                            toShip,
                            com.kis.wmsapplication.modules.inventoryModule.enums.MovementType.SHIPMENT,
                            "Отгрузка заказа №" + order.getId() + " клиенту " + order.getCustomer().getName()
                    );
                    
                    remainingToShip = remainingToShip.subtract(toShip);
                }
            }
            
            // Предупреждаем если не удалось отгрузить полностью
            if (remainingToShip.compareTo(BigDecimal.ZERO) > 0) {
                log.warn("Не удалось отгрузить полностью товар {} для заказа #{}: осталось {}",
                        item.getProduct().getSku(), orderId, remainingToShip);
            }
        }

        order.setStatus(SalesStatus.SHIPPED);
        order.setShippedAt(Instant.now());
        orderRepository.save(order);
        
        log.info("Заказ #{} отгружен клиенту '{}'", orderId, order.getCustomer().getName());
    }

    /**
     * Отмена заказа - освобождение резерва
     * 
     * @param orderId ID заказа
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        OutgoingOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ с ID " + orderId + " не найден"));

        if (order.getStatus() == SalesStatus.SHIPPED) {
            throw new IllegalStateException("Нельзя отменить уже отгруженный заказ");
        }

        // Освобождаем резерв
        for (OutgoingOrderItem item : order.getItems()) {
            if (item.getReservedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                List<ProductLocationStock> stocks = inventoryService.findStocksByProduct(item.getProduct().getId());
                
                BigDecimal remainingToRelease = item.getReservedQuantity();
                
                for (ProductLocationStock stock : stocks) {
                    if (remainingToRelease.compareTo(BigDecimal.ZERO) <= 0) break;
                    
                    BigDecimal reservedInStock = stock.getReserved();
                    if (reservedInStock.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal toRelease = reservedInStock.min(remainingToRelease);
                        
                        stock.setReserved(stock.getReserved().subtract(toRelease));
                        inventoryService.saveStock(stock);
                        
                        remainingToRelease = remainingToRelease.subtract(toRelease);
                    }
                }
            }
        }

        order.setStatus(SalesStatus.CANCELLED);
        orderRepository.save(order);
        
        log.info("Заказ #{} отменен, резерв освобожден", orderId);
    }
}
