package com.kis.wmsapplication.modules.inventoryModule.service;

import com.kis.wmsapplication.modules.catalogModule.model.Product;
import com.kis.wmsapplication.modules.catalogModule.repository.ProductRepository;
import com.kis.wmsapplication.modules.inventoryModule.dto.StocktakeRequest;
import com.kis.wmsapplication.modules.inventoryModule.dto.StocktakeResult;
import com.kis.wmsapplication.modules.inventoryModule.enums.MovementType;
import com.kis.wmsapplication.modules.inventoryModule.model.ProductLocationStock;
import com.kis.wmsapplication.modules.inventoryModule.repository.StockRepository;
import com.kis.wmsapplication.modules.userModule.Exception.ResourceNotFoundException;
import com.kis.wmsapplication.modules.warehouseModule.model.HierarchyLevel;
import com.kis.wmsapplication.modules.warehouseModule.repository.HierarchyLevelRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис проведения инвентаризации (Stocktake).
 * Бизнес-процесс:
 *   1. Получить «учётные» остатки по локации
 *   2. Сравнить с фактически подсчитанными значениями
 *   3. Для каждого расхождения создать движение ADJUSTMENT
 *   4. Вернуть отчёт с расхождениями
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StocktakeService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final HierarchyLevelRepository locationRepository;
    private final InventoryService inventoryService;

    @Transactional
    public StocktakeResult perform(StocktakeRequest request) {
        if (request == null || request.locationId() == null) {
            throw new IllegalArgumentException("Не указана локация для инвентаризации");
        }
        HierarchyLevel location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new ResourceNotFoundException("Локация не найдена"));

        List<ProductLocationStock> existing = stockRepository.findAllByLocationId(request.locationId());
        Map<Long, ProductLocationStock> stocksByProduct = new HashMap<>();
        for (ProductLocationStock s : existing) {
            stocksByProduct.put(s.getProduct().getId(), s);
        }

        List<StocktakeResult.StocktakeLine> lines = new ArrayList<>();
        int discrepancies = 0;
        BigDecimal totalAdjustment = BigDecimal.ZERO;

        List<StocktakeRequest.StocktakeItem> items = request.items() != null ? request.items() : List.of();
        for (StocktakeRequest.StocktakeItem item : items) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Товар не найден: " + item.productId()));

            ProductLocationStock current = stocksByProduct.remove(item.productId());
            BigDecimal systemQty = current != null ? current.getQuantity() : BigDecimal.ZERO;
            BigDecimal countedQty = item.countedQuantity() == null ? BigDecimal.ZERO : item.countedQuantity();
            BigDecimal diff = countedQty.subtract(systemQty);

            String action = "OK";
            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                discrepancies++;
                totalAdjustment = totalAdjustment.add(diff.abs());

                if (current == null) {
                    // Товара в системе не было, фактически найден
                    inventoryService.createMovement(product, null, location, countedQty,
                            MovementType.ADJUSTMENT,
                            buildReference(request.reference(), "found"));
                    action = "ADD";
                } else if (diff.compareTo(BigDecimal.ZERO) > 0) {
                    // Фактически больше — оприходовать
                    inventoryService.createMovement(product, null, location, diff,
                            MovementType.ADJUSTMENT,
                            buildReference(request.reference(), "surplus"));
                    action = "SURPLUS";
                } else {
                    // Фактически меньше — списать недостачу
                    inventoryService.createMovement(product, location, null, diff.abs(),
                            MovementType.ADJUSTMENT,
                            buildReference(request.reference(), "shortage"));
                    action = "SHORTAGE";
                }
                // Применяем фактический остаток
                applyCounted(location, product, countedQty);
            }

            lines.add(new StocktakeResult.StocktakeLine(
                    product.getId(),
                    product.getSku(),
                    product.getName(),
                    systemQty,
                    countedQty,
                    diff,
                    action
            ));
        }

        // Товары, которые есть в системе, но не были посчитаны — оставляем без изменений

        log.info("Инвентаризация локации {}: проверено {}, расхождений {}",
                location.getCode(), lines.size(), discrepancies);

        return new StocktakeResult(
                location.getId(),
                location.getCode(),
                lines.size(),
                discrepancies,
                totalAdjustment,
                lines
        );
    }

    /**
     * Получить текущие остатки в локации для проведения инвентаризации.
     */
    public List<StocktakeResult.StocktakeLine> getLocationStock(Long locationId) {
        List<ProductLocationStock> existing = stockRepository.findAllByLocationId(locationId);
        List<StocktakeResult.StocktakeLine> result = new ArrayList<>();
        for (ProductLocationStock s : existing) {
            result.add(new StocktakeResult.StocktakeLine(
                    s.getProduct().getId(),
                    s.getProduct().getSku(),
                    s.getProduct().getName(),
                    s.getQuantity(),
                    s.getQuantity(),
                    BigDecimal.ZERO,
                    "PENDING"
            ));
        }
        return result;
    }

    private void applyCounted(HierarchyLevel location, Product product, BigDecimal countedQty) {
        ProductLocationStock stock = stockRepository.findByLocationIdAndProductId(location.getId(), product.getId())
                .orElseGet(() -> {
                    ProductLocationStock s = ProductLocationStock.builder()
                            .location(location)
                            .product(product)
                            .quantity(BigDecimal.ZERO)
                            .reserved(BigDecimal.ZERO)
                            .build();
                    s.setId(new com.kis.wmsapplication.modules.inventoryModule.model.ProductLocationStockId(location.getId(), product.getId()));
                    return s;
                });
        stock.setQuantity(countedQty);
        if (stock.getReserved() != null && stock.getReserved().compareTo(countedQty) > 0) {
            stock.setReserved(countedQty);
        }
        stockRepository.save(stock);
    }

    private String buildReference(String userRef, String kind) {
        String base = userRef == null || userRef.isBlank() ? "STOCKTAKE" : userRef;
        return base + ":" + kind;
    }
}
