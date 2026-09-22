package com.kis.wmsapplication.modules.inventoryModule.service;


import com.kis.wmsapplication.modules.catalogModule.model.Product;
import com.kis.wmsapplication.modules.catalogModule.repository.ProductRepository;
import com.kis.wmsapplication.modules.inventoryModule.dto.StockOperationDto;
import com.kis.wmsapplication.modules.inventoryModule.model.InventoryMovement;
import com.kis.wmsapplication.modules.inventoryModule.model.ProductLocationStock;
import com.kis.wmsapplication.modules.inventoryModule.model.ProductLocationStockId;
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
    public BigDecimal reserveStock(Long productId, BigDecimal requestedQty) {

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

        MovementType type = request.type() != null ? request.type() : MovementType.ADJUSTMENT;
        
        // Валидация локаций в зависимости от типа операции
        validateOperationLocations(request, type);

        HierarchyLevel fromLocation = null;
        HierarchyLevel toLocation = null;

        // Загружаем локации только если они требуются для данного типа операции
        if (request.fromLocationId() != null && needsFromLocation(type)) {
            fromLocation = locationRepository.findById(request.fromLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Исходная локация не найдена"));
        }
        if (request.toLocationId() != null && needsToLocation(type)) {
            toLocation = locationRepository.findById(request.toLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Целевая локация не найдена"));
        }

        // 1. Логика списания (для SHIPMENT и TRANSFER)
        if (fromLocation != null && (type == MovementType.SHIPMENT || type == MovementType.TRANSFER)) {
            decreaseStock(fromLocation, product, request.quantity());
        }

        // 2. Логика зачисления (для RECEIPT, TRANSFER и ADJUSTMENT)
        if (toLocation != null && (type == MovementType.RECEIPT || type == MovementType.TRANSFER || type == MovementType.ADJUSTMENT)) {
            increaseStock(toLocation, product, request.quantity());
        }

        // 3. Запись в историю движений (Audit Log)
        InventoryMovement movement = InventoryMovement.builder()
                .product(product)
                .fromLocation(fromLocation)
                .toLocation(toLocation)
                .quantity(request.quantity())
                .type(type)
                .reference(request.reference())
                .build();

        movementRepository.save(movement);
    }
    
    /**
     * Валидация наличия необходимых локаций для типа операции
     */
    private void validateOperationLocations(StockOperationDto request, MovementType type) {
        switch (type) {
            case RECEIPT:
                if (request.toLocationId() == null) {
                    throw new IllegalArgumentException("Для приемки необходимо указать целевую локацию");
                }
                break;
            case SHIPMENT:
                if (request.fromLocationId() == null) {
                    throw new IllegalArgumentException("Для отгрузки необходимо указать исходную локацию");
                }
                break;
            case TRANSFER:
                if (request.fromLocationId() == null) {
                    throw new IllegalArgumentException("Для перемещения необходимо указать исходную локацию");
                }
                if (request.toLocationId() == null) {
                    throw new IllegalArgumentException("Для перемещения необходимо указать целевую локацию");
                }
                if (request.fromLocationId().equals(request.toLocationId())) {
                    throw new IllegalArgumentException("Исходная и целевая локации не могут совпадать");
                }
                break;
            case ADJUSTMENT:
                if (request.toLocationId() == null) {
                    throw new IllegalArgumentException("Для корректировки необходимо указать локацию");
                }
                break;
        }
    }
    
    private boolean needsFromLocation(MovementType type) {
        return type == MovementType.SHIPMENT || type == MovementType.TRANSFER;
    }
    
    private boolean needsToLocation(MovementType type) {
        return type == MovementType.RECEIPT || type == MovementType.TRANSFER || type == MovementType.ADJUSTMENT;
    }


    private void decreaseStock(HierarchyLevel location, Product product, BigDecimal quantity) {
        // Проверка на отрицательное количество
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Количество для списания должно быть больше нуля");
        }

        ProductLocationStock stock = stockRepository.findByLocationIdAndProductId(location.getId(), product.getId())
                .orElseThrow(() -> new IllegalArgumentException("В ячейке %s нет товара %s".formatted(location.getCode(), product.getSku())));

        if (stock.getQuantity().compareTo(quantity) < 0) {
            throw new IllegalArgumentException("Недостаточно товара в ячейке. Доступно: " + stock.getQuantity() + ", требуется: " + quantity);
        }

        stock.setQuantity(stock.getQuantity().subtract(quantity));
        
        // Проверка на отрицательное значение после списания
        if (stock.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Отрицательное количество товара после списания");
        }
        
        stockRepository.save(stock);
    }

    private void increaseStock(HierarchyLevel location, Product product, BigDecimal quantity) {
        // Проверка на отрицательное количество
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Количество для добавления должно быть больше нуля");
        }

        ProductLocationStock stock = stockRepository.findByLocationIdAndProductId(location.getId(), product.getId())
                .orElseGet(() -> {
                    // Создаем новый stock с инициализированным embedded ID
                    ProductLocationStock newStock = ProductLocationStock.builder()
                            .location(location)
                            .product(product)
                            .quantity(BigDecimal.ZERO)
                            .reserved(BigDecimal.ZERO)
                            .build();
                    // Инициализируем embedded ID явно перед сохранением
                    newStock.setId(new ProductLocationStockId(location.getId(), product.getId()));
                    return newStock;
                });

        stock.setQuantity(stock.getQuantity().add(quantity));
        stockRepository.save(stock);
    }

    public List<ProductLocationStock> findStocksByProduct(Long productId) {
        return stockRepository.findAllByProductId(productId);
    }

    public void saveStock(ProductLocationStock stock) {
        stockRepository.save(stock);
    }

    public void createMovement(Product product, HierarchyLevel fromLocation, HierarchyLevel toLocation, 
                              BigDecimal quantity, MovementType type, String reference) {
        InventoryMovement movement = InventoryMovement.builder()
                .product(product)
                .fromLocation(fromLocation)
                .toLocation(toLocation)
                .quantity(quantity)
                .type(type)
                .reference(reference)
                .build();
        movementRepository.save(movement);
    }
}