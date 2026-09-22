package com.kis.wmsapplication.modules.inventoryModule.service;

import com.kis.wmsapplication.modules.catalogModule.model.Product;
import com.kis.wmsapplication.modules.catalogModule.repository.ProductRepository;
import com.kis.wmsapplication.modules.inventoryModule.dto.StockSummaryDto;
import com.kis.wmsapplication.modules.inventoryModule.model.ProductLocationStock;
import com.kis.wmsapplication.modules.inventoryModule.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Централизованный сервис для работы с остатками товаров
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Получить сводку по остаткам всех товаров
     */
    @Transactional(readOnly = true)
    public List<StockSummaryDto> getAllStockSummaries() {
        // Один запрос для получения всех остатков с продуктами
        String sql = """
            SELECT 
                p.id as product_id,
                p.sku,
                p.name,
                p.min_stock,
                p.max_stock,
                p.reorder_point,
                p.eoq,
                COALESCE(SUM(s.quantity), 0) as total_quantity,
                COALESCE(SUM(s.reserved), 0) as total_reserved,
                COALESCE(
                    (SELECT AVG(daily_qty) FROM (
                        SELECT DATE(o.created_at) as sale_date, SUM(oi.quantity) as daily_qty
                        FROM outgoing_order o
                        JOIN outgoing_order_item oi ON o.id = oi.order_id
                        WHERE oi.product_id = p.id 
                          AND o.status IN ('SHIPPED', 'RESERVED')
                          AND o.created_at >= NOW() - INTERVAL '30 days'
                        GROUP BY DATE(o.created_at)
                    ) daily_sales), 0
                ) as avg_daily_sales
            FROM product p
            LEFT JOIN product_location_stock s ON p.id = s.product_id
            WHERE p.active = true
            GROUP BY p.id, p.sku, p.name, p.min_stock, p.max_stock, p.reorder_point, p.eoq
            ORDER BY p.name
            """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        
        // Получаем детализацию по локациям для всех продуктов (один запрос)
        Map<Long, List<StockSummaryDto.LocationStockDto>> locationStocksMap = getLocationStocksForAllProducts();

        return results.stream()
                .map(row -> {
                    Long productId = ((Number) row.get("product_id")).longValue();
                    
                    return StockSummaryDto.create(
                            productId,
                            (String) row.get("sku"),
                            (String) row.get("name"),
                            getBigDecimal(row.get("total_quantity")),
                            getBigDecimal(row.get("total_reserved")),
                            getBigDecimal(row.get("min_stock")),
                            getBigDecimal(row.get("max_stock")),
                            getBigDecimal(row.get("reorder_point")),
                            getBigDecimal(row.get("eoq")),
                            getBigDecimal(row.get("avg_daily_sales")),
                            locationStocksMap.getOrDefault(productId, Collections.emptyList())
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Получить сводку по остаткам конкретного товара
     */
    @Transactional(readOnly = true)
    public StockSummaryDto getStockSummary(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Товар с ID " + productId + " не найден"));

        List<ProductLocationStock> stocks = stockRepository.findAllByProductId(productId);
        
        BigDecimal totalQuantity = stocks.stream()
                .map(ProductLocationStock::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalReserved = stocks.stream()
                .map(ProductLocationStock::getReserved)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<StockSummaryDto.LocationStockDto> locationStocks = stocks.stream()
                .map(s -> new StockSummaryDto.LocationStockDto(
                        s.getLocation().getId(),
                        s.getLocation().getCode(),
                        s.getLocation().getName(),
                        s.getLocation().getCategory() != null ? s.getLocation().getCategory().getLevelName() : null,
                        s.getQuantity(),
                        s.getReserved(),
                        s.getQuantity().subtract(s.getReserved())
                ))
                .collect(Collectors.toList());

        // Расчет средних дневных продаж
        BigDecimal avgDailySales = calculateAverageDailySales(productId, 30);

        return StockSummaryDto.create(
                product.getId(),
                product.getSku(),
                product.getName(),
                totalQuantity,
                totalReserved,
                product.getMinStock(),
                product.getMaxStock(),
                product.getReorderPoint(),
                product.getEoq(),
                avgDailySales,
                locationStocks
        );
    }

    /**
     * Получить товары с низким остатком
     */
    @Transactional(readOnly = true)
    public List<StockSummaryDto> getLowStockProducts() {
        return getAllStockSummaries().stream()
                .filter(s -> s.status() == StockSummaryDto.StockStatus.LOW 
                          || s.status() == StockSummaryDto.StockStatus.CRITICAL
                          || s.status() == StockSummaryDto.StockStatus.OUT_OF_STOCK)
                .collect(Collectors.toList());
    }

    /**
     * Получить товары без остатка
     */
    @Transactional(readOnly = true)
    public List<StockSummaryDto> getOutOfStockProducts() {
        return getAllStockSummaries().stream()
                .filter(s -> s.status() == StockSummaryDto.StockStatus.OUT_OF_STOCK)
                .collect(Collectors.toList());
    }

    /**
     * Получить товары с избыточным остатком
     */
    @Transactional(readOnly = true)
    public List<StockSummaryDto> getExcessStockProducts() {
        return getAllStockSummaries().stream()
                .filter(s -> s.status() == StockSummaryDto.StockStatus.EXCESS)
                .collect(Collectors.toList());
    }

    /**
     * Агрегированная статистика по остаткам
     */
    @Transactional(readOnly = true)
    public StockStatistics getStockStatistics() {
        List<StockSummaryDto> allStocks = getAllStockSummaries();
        
        long totalProducts = allStocks.size();
        long outOfStock = allStocks.stream()
                .filter(s -> s.status() == StockSummaryDto.StockStatus.OUT_OF_STOCK).count();
        long criticalStock = allStocks.stream()
                .filter(s -> s.status() == StockSummaryDto.StockStatus.CRITICAL).count();
        long lowStock = allStocks.stream()
                .filter(s -> s.status() == StockSummaryDto.StockStatus.LOW).count();
        long normalStock = allStocks.stream()
                .filter(s -> s.status() == StockSummaryDto.StockStatus.NORMAL).count();
        long excessStock = allStocks.stream()
                .filter(s -> s.status() == StockSummaryDto.StockStatus.EXCESS).count();
        
        BigDecimal totalValue = allStocks.stream()
                .map(s -> {
                    // Получаем цену продукта
                    BigDecimal price = productRepository.findById(s.productId())
                            .map(Product::getPrice)
                            .orElse(BigDecimal.ZERO);
                    return s.totalQuantity().multiply(price);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDeficit = allStocks.stream()
                .map(StockSummaryDto::deficit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StockStatistics(
                totalProducts,
                outOfStock,
                criticalStock,
                lowStock,
                normalStock,
                excessStock,
                totalValue,
                totalDeficit
        );
    }


    private Map<Long, List<StockSummaryDto.LocationStockDto>> getLocationStocksForAllProducts() {
        String sql = """
            SELECT 
                s.product_id,
                s.location_id,
                h.code as location_code,
                h.name as location_name,
                c.level as location_type,
                s.quantity,
                s.reserved
            FROM product_location_stock s
            JOIN warehouse_hierarchy_level h ON s.location_id = h.id
            LEFT JOIN warehouse_hierarchy_level_category c ON h.level_id = c.id
            ORDER BY s.product_id, h.code
            """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        
        return results.stream()
                .collect(Collectors.groupingBy(
                        row -> ((Number) row.get("product_id")).longValue(),
                        Collectors.mapping(
                                row -> new StockSummaryDto.LocationStockDto(
                                        ((Number) row.get("location_id")).longValue(),
                                        (String) row.get("location_code"),
                                        (String) row.get("location_name"),
                                        (String) row.get("location_type"),
                                        getBigDecimal(row.get("quantity")),
                                        getBigDecimal(row.get("reserved")),
                                        getBigDecimal(row.get("quantity")).subtract(getBigDecimal(row.get("reserved")))
                                ),
                                Collectors.toList()
                        )
                ));
    }

    private BigDecimal calculateAverageDailySales(Long productId, int days) {
        String sql = """
            SELECT COALESCE(AVG(daily_qty), 0) as avg_daily
            FROM (
                SELECT DATE(o.created_at) as sale_date, SUM(oi.quantity) as daily_qty
                FROM outgoing_order o
                JOIN outgoing_order_item oi ON o.id = oi.order_id
                WHERE oi.product_id = ?
                  AND o.status IN ('SHIPPED', 'RESERVED')
                  AND o.created_at >= NOW() - CAST(? || ' days' AS INTERVAL)
                GROUP BY DATE(o.created_at)
            ) daily_sales
            """;

        try {
            BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, productId, days);
            return result != null ? result : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Failed to calculate average daily sales for product {}: {}", productId, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return BigDecimal.ZERO;
    }


    public record StockStatistics(
            long totalProducts,
            long outOfStock,
            long criticalStock,
            long lowStock,
            long normalStock,
            long excessStock,
            BigDecimal totalStockValue,
            BigDecimal totalDeficit
    ) {}
}

