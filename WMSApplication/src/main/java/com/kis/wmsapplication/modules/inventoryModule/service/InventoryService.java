package com.kis.wmsapplication.modules.inventoryModule.service;


import com.kis.wmsapplication.modules.catalogModule.model.Product;
import com.kis.wmsapplication.modules.catalogModule.repository.ProductRepository;
import com.kis.wmsapplication.modules.inventoryModule.dto.StockOperationDto;
import com.kis.wmsapplication.modules.inventoryModule.model.InventoryMovement;
import com.kis.wmsapplication.modules.inventoryModule.model.ProductLocationStock;
import com.kis.wmsapplication.modules.inventoryModule.enums.MovementType;
import com.kis.wmsapplication.modules.inventoryModule.repository.MovementRepository;
import com.kis.wmsapplication.modules.inventoryModule.repository.StockRepository;
import com.kis.wmsapplication.modules.userModule.Exception.ResourceNotFoundException;
import com.kis.wmsapplication.modules.warehouseModule.model.HierarchyLevel;
import com.kis.wmsapplication.modules.warehouseModule.repository.HierarchyLevelRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final StockRepository stockRepository;
    private final MovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final HierarchyLevelRepository locationRepository;
    @Transactional
    public BigDecimal reserveStock(UUID productId, BigDecimal requestedQty) {

        List<ProductLocationStock> stocks = stockRepository.findAllByProductId(productId);

        BigDecimal remainingToReserve = requestedQty;
        BigDecimal totalReserved = BigDecimal.ZERO;

        for (ProductLocationStock stock : stocks) {
            if (remainingToReserve.compareTo(BigDecimal.ZERO) <= 0) break;

            // Доступно = Физически - Уже_Зарезервировано
            BigDecimal available = stock.getQuantity().subtract(stock.getReserved());

            if (available.compareTo(BigDecimal.ZERO) > 0) {
                // Берем либо сколько надо, либо всё что есть свободного
                BigDecimal take = available.min(remainingToReserve);

                stock.setReserved(stock.getReserved().add(take));
                stockRepository.save(stock);

                remainingToReserve = remainingToReserve.subtract(take);
                totalReserved = totalReserved.add(take);
            }
        }

        return totalReserved;
    }
    @Transactional
    public void processOperation(StockOperationDto request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден"));

        HierarchyLevel fromLocation = null;
        HierarchyLevel toLocation = null;

        if (request.fromLocationId() != null) {
            fromLocation = locationRepository.findById(request.fromLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ячейка отправитель не найдена"));
        }
        if (request.toLocationId() != null) {
            toLocation = locationRepository.findById(request.toLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ячейка получатель не найдена"));
        }

        // 1. Логика списания (если есть откуда)
        if (fromLocation != null) {
            decreaseStock(fromLocation, product, request.quantity());
        }

        // 2. Логика зачисления (если есть куда)
        if (toLocation != null) {
            increaseStock(toLocation, product, request.quantity());
        }

        // 3. Запись в историю движений (Audit Log)
        InventoryMovement movement = InventoryMovement.builder()
                .product(product)
                .fromLocation(fromLocation)
                .toLocation(toLocation)
                .quantity(request.quantity())
                .type(request.type() != null ? request.type() : MovementType.ADJUSTMENT)
                .reference(request.reference())
                .build();

        movementRepository.save(movement);
    }

    private void increaseStock(HierarchyLevel location, Product product, BigDecimal quantity) {
        ProductLocationStock stock = stockRepository.findByLocationIdAndProductId(location.getId(), product.getId())
                .orElse(ProductLocationStock.builder()
                        .location(location)
                        .product(product)
                        .quantity(BigDecimal.ZERO)
                        .reserved(BigDecimal.ZERO)
                        .build());

        stock.setQuantity(stock.getQuantity().add(quantity));
        stockRepository.save(stock);
    }

    private void decreaseStock(HierarchyLevel location, Product product, BigDecimal quantity) {
        ProductLocationStock stock = stockRepository.findByLocationIdAndProductId(location.getId(), product.getId())
                .orElseThrow(() -> new IllegalArgumentException("В ячейке %s нет товара %s".formatted(location.getCode(), product.getSku())));

        if (stock.getQuantity().compareTo(quantity) < 0) {
            throw new IllegalArgumentException("Недостаточно товара в ячейке. Доступно: " + stock.getQuantity());
        }

        stock.setQuantity(stock.getQuantity().subtract(quantity));
        stockRepository.save(stock);
    }
}