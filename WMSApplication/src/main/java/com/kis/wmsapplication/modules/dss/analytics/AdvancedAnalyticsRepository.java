package com.kis.wmsapplication.modules.dss.analytics;

import com.kis.wmsapplication.modules.dss.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AdvancedAnalyticsRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<StockReportDto> getStockReport() {
        String sql = """
            SELECT 
                p.id as product_id,
                p.sku,
                p.name,
                COALESCE(SUM(s.quantity), 0) as current_stock,
                COALESCE(SUM(s.reserved), 0) as reserved_stock,
                COALESCE(SUM(s.quantity), 0) - COALESCE(SUM(s.reserved), 0) as available_stock,
                p.min_stock,
                p.reorder_point,
                p.max_stock,
                (COALESCE(SUM(s.quantity), 0) * p.unit_price) as stock_value,
                p.active
            FROM product p
            LEFT JOIN product_location_stock s ON p.id = s.product_id
            WHERE p.active = true
            GROUP BY p.id, p.sku, p.name, p.min_stock, p.reorder_point, p.max_stock, p.unit_price
            ORDER BY stock_value DESC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            BigDecimal currentStock = rs.getBigDecimal("current_stock");
            BigDecimal reservedStock = rs.getBigDecimal("reserved_stock");
            BigDecimal availableStock = rs.getBigDecimal("available_stock");
            BigDecimal reorderPoint = rs.getBigDecimal("reorder_point");
            BigDecimal minStock = rs.getBigDecimal("min_stock");
            
            if (currentStock == null) currentStock = BigDecimal.ZERO;
            if (reservedStock == null) reservedStock = BigDecimal.ZERO;
            if (availableStock == null) availableStock = BigDecimal.ZERO;
            
            String status = "NORMAL";
            if (reorderPoint != null && availableStock.compareTo(reorderPoint) < 0) {
                status = availableStock.compareTo(BigDecimal.ZERO) <= 0 ? "OUT" : "LOW";
            } else if (minStock != null && availableStock.compareTo(minStock.multiply(new BigDecimal("1.5"))) > 0) {
                status = "EXCESS";
            }
            
            return new StockReportDto(
                    rs.getLong("product_id"),
                    rs.getString("sku"),
                    rs.getString("name"),
                    currentStock,
                    reservedStock,
                    availableStock,
                    rs.getBigDecimal("min_stock"),
                    reorderPoint,
                    rs.getBigDecimal("max_stock"),
                    rs.getBigDecimal("stock_value"),
                    BigDecimal.ZERO, // turnoverRate будет рассчитан отдельно
                    0, // daysOfStock будет рассчитан отдельно
                    status
            );
        });
    }

    public List<MovementHistoryDto> getMovementHistory(Long productId, Instant fromDate, Instant toDate) {
        // Строим SQL динамически в зависимости от переданных параметров
        StringBuilder sqlBuilder = new StringBuilder("""
        SELECT 
            m.id,
            m.product_id,
            p.sku,
            p.name,
            m.quantity,
            fl.code as from_location_code,
            fl.name as from_location_name,
            tl.code as to_location_code,
            tl.name as to_location_name,
            m.movement_type,
            m.occurred_at,
            m.reference
        FROM inventory_movement m
        JOIN product p ON m.product_id = p.id
        LEFT JOIN warehouse_hierarchy_level fl ON m.from_location_id = fl.id
        LEFT JOIN warehouse_hierarchy_level tl ON m.to_location_id = tl.id
        WHERE 1=1
        """);

        MapSqlParameterSource params = new MapSqlParameterSource();

        if (productId != null) {
            sqlBuilder.append(" AND m.product_id = :productId");
            params.addValue("productId", productId, Types.BIGINT);
        }

        if (fromDate != null) {
            sqlBuilder.append(" AND m.occurred_at >= :fromDate");
            params.addValue("fromDate", Timestamp.from(fromDate), Types.TIMESTAMP);
        }

        if (toDate != null) {
            sqlBuilder.append(" AND m.occurred_at <= :toDate");
            params.addValue("toDate", Timestamp.from(toDate), Types.TIMESTAMP);
        }

        sqlBuilder.append(" ORDER BY m.occurred_at DESC LIMIT 1000");
        
        String sql = sqlBuilder.toString();

        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> {
            Timestamp occurredAtTimestamp = rs.getTimestamp("occurred_at");
            Instant occurredAt = occurredAtTimestamp != null ? occurredAtTimestamp.toInstant() : null;
            
            return new MovementHistoryDto(
                    rs.getLong("id"),
                    rs.getLong("product_id"),
                    rs.getString("sku"),
                    rs.getString("name"),
                    rs.getBigDecimal("quantity"),
                    rs.getString("from_location_code"),
                    rs.getString("from_location_name"),
                    rs.getString("to_location_code"),
                    rs.getString("to_location_name"),
                    rs.getString("movement_type"),
                    occurredAt,
                    rs.getString("reference")
            );
        });
    }

    public List<ABCXYZAnalysisDto> getABCXYZAnalysis(int days) {
        String sql = """
            WITH sales_data AS (
                SELECT 
                    oi.product_id,
                    SUM(oi.quantity * oi.unit_price) as total_sales_value,
                    SUM(oi.quantity) as total_sales_quantity
                FROM outgoing_order_item oi
                JOIN outgoing_order o ON oi.order_id = o.id
                WHERE o.created_at >= NOW() - (? * INTERVAL '1 day')
                  AND o.status = 'SHIPPED'::sales_status
                GROUP BY oi.product_id
            ),
            stock_data AS (
                SELECT 
                    p.id as product_id,
                    COALESCE(SUM(s.quantity), 0) as avg_stock
                FROM product p
                LEFT JOIN product_location_stock s ON p.id = s.product_id
                WHERE p.active = true
                GROUP BY p.id
            )
            SELECT 
                p.id as product_id,
                p.sku,
                p.name,
                COALESCE(sd.total_sales_value, 0) as total_sales_value,
                COALESCE(sd.total_sales_quantity, 0) as total_sales_quantity,
                COALESCE(st.avg_stock, 0) as average_stock,
                CASE 
                    WHEN st.avg_stock > 0 THEN COALESCE(sd.total_sales_quantity, 0) / st.avg_stock
                    ELSE 0
                END as turnover_rate
            FROM product p
            LEFT JOIN sales_data sd ON p.id = sd.product_id
            LEFT JOIN stock_data st ON p.id = st.product_id
            WHERE p.active = true
            ORDER BY sd.total_sales_value DESC NULLS LAST
        """;

        List<ABCXYZAnalysisDto> products = jdbcTemplate.query(sql, (rs, rowNum) -> {
            BigDecimal salesValue = rs.getBigDecimal("total_sales_value");
            BigDecimal avgStock = rs.getBigDecimal("average_stock");
            BigDecimal turnoverRate = rs.getBigDecimal("turnover_rate");
            BigDecimal totalSalesQuantity = rs.getBigDecimal("total_sales_quantity");
            
            // Обработка null значений
            if (salesValue == null) salesValue = BigDecimal.ZERO;
            if (avgStock == null) avgStock = BigDecimal.ZERO;
            if (turnoverRate == null) turnoverRate = BigDecimal.ZERO;
            if (totalSalesQuantity == null) totalSalesQuantity = BigDecimal.ZERO;
            
            return new ABCXYZAnalysisDto(
                    rs.getLong("product_id"),
                    rs.getString("sku"),
                    rs.getString("name"),
                    salesValue,
                    totalSalesQuantity,
                    avgStock,
                    turnoverRate,
                    null, // ABC будет рассчитан в сервисе
                    null, // XYZ будет рассчитан в сервисе
                    null  // recommendation будет рассчитан в сервисе
            );
        }, days);

        // Рассчитываем ABC категории
        BigDecimal totalValue = products.stream()
                .map(ABCXYZAnalysisDto::totalSalesValue)
                .map(value -> value != null ? value : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal cumulative = BigDecimal.ZERO;
            for (int i = 0; i < products.size(); i++) {
                ABCXYZAnalysisDto product = products.get(i);
                BigDecimal salesValue = product.totalSalesValue() != null ? product.totalSalesValue() : BigDecimal.ZERO;
                cumulative = cumulative.add(salesValue);
                BigDecimal percentage = cumulative.divide(totalValue, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                
                String abcCategory = "C";
                if (percentage.compareTo(new BigDecimal("80")) <= 0) {
                    abcCategory = "A";
                } else if (percentage.compareTo(new BigDecimal("95")) <= 0) {
                    abcCategory = "B";
                }
                
                // XYZ категория на основе оборачиваемости
                String xyzCategory = "Z";
                BigDecimal turnover = product.turnoverRate() != null ? product.turnoverRate() : BigDecimal.ZERO;
                if (turnover.compareTo(new BigDecimal("12")) >= 0) {
                    xyzCategory = "X"; // Высокая оборачиваемость (>12 раз в год)
                } else if (turnover.compareTo(new BigDecimal("4")) >= 0) {
                    xyzCategory = "Y"; // Средняя оборачиваемость (4-12 раз)
                }
                
                String recommendation = generateRecommendation(abcCategory, xyzCategory);
                
                products.set(i, new ABCXYZAnalysisDto(
                        product.productId(),
                        product.sku(),
                        product.name(),
                        product.totalSalesValue(),
                        product.totalSalesQuantity(),
                        product.averageStock(),
                        product.turnoverRate(),
                        abcCategory,
                        xyzCategory,
                        recommendation
                ));
            }
        }

        return products;
    }

    private String generateRecommendation(String abc, String xyz) {
        if ("AX".equals(abc + xyz)) return "Критически важный товар с высокой оборачиваемостью. Требует постоянного контроля и быстрой поставки.";
        if ("AY".equals(abc + xyz)) return "Важный товар со средней оборачиваемостью. Регулярный мониторинг и планирование закупок.";
        if ("AZ".equals(abc + xyz)) return "Важный товар с низкой оборачиваемостью. Проверить необходимость в больших запасах.";
        if ("BX".equals(abc + xyz)) return "Средневажный товар с высокой оборачиваемостью. Стандартное управление запасами.";
        if ("BY".equals(abc + xyz)) return "Средневажный товар со средней оборачиваемостью. Периодический контроль.";
        if ("BZ".equals(abc + xyz)) return "Средневажный товар с низкой оборачиваемостью. Минимизировать запасы.";
        if ("CX".equals(abc + xyz)) return "Маловажный товар с высокой оборачиваемостью. Упрощенное управление.";
        if ("CY".equals(abc + xyz)) return "Маловажный товар со средней оборачиваемостью. Минимальный контроль.";
        if ("CZ".equals(abc + xyz)) return "Маловажный товар с низкой оборачиваемостью. Рассмотреть возможность исключения из ассортимента.";
        return "Стандартное управление запасами.";
    }

    public List<SupplierPerformanceDto> getSupplierPerformance(int days) {
        String sql = """
            SELECT 
                s.id as supplier_id,
                s.name as supplier_name,
                COUNT(DISTINCT o.id) as total_orders,
                COUNT(DISTINCT CASE WHEN o.status = 'RECEIVED' THEN o.id END) as completed_orders,
                COUNT(DISTINCT CASE WHEN o.status = 'IN_TRANSIT' AND o.expected_arrival < NOW() THEN o.id END) as delayed_orders,
                COALESCE(SUM(oi.quantity * oi.purchase_price), 0) as total_order_value,
                COALESCE(AVG(oi.quantity * oi.purchase_price), 0) as avg_order_value,
                COALESCE(AVG(s.avg_lead_time_days), 0) as avg_lead_time,
                MAX(o.order_date) as last_order_date
            FROM supplier s
            LEFT JOIN incoming_order o ON s.id = o.supplier_id 
                AND o.order_date >= NOW() - (? * INTERVAL '1 day')
            LEFT JOIN incoming_order_item oi ON o.id = oi.order_id
            GROUP BY s.id, s.name
            HAVING COUNT(DISTINCT o.id) > 0
            ORDER BY total_order_value DESC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            int totalOrders = rs.getInt("total_orders");
            int completedOrders = rs.getInt("completed_orders");
            BigDecimal onTimeRate = totalOrders > 0 
                    ? new BigDecimal(completedOrders).divide(new BigDecimal(totalOrders), 4, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            
            String rating = "POOR";
            if (onTimeRate.compareTo(new BigDecimal("0.95")) >= 0) {
                rating = "EXCELLENT";
            } else if (onTimeRate.compareTo(new BigDecimal("0.80")) >= 0) {
                rating = "GOOD";
            } else if (onTimeRate.compareTo(new BigDecimal("0.60")) >= 0) {
                rating = "AVERAGE";
            }
            
            return new SupplierPerformanceDto(
                    rs.getLong("supplier_id"),
                    rs.getString("supplier_name"),
                    totalOrders,
                    completedOrders,
                    rs.getInt("delayed_orders"),
                    rs.getBigDecimal("total_order_value"),
                    rs.getBigDecimal("avg_order_value"),
                    rs.getBigDecimal("avg_lead_time"),
                    onTimeRate.multiply(new BigDecimal("100")),
                    rs.getTimestamp("last_order_date") != null 
                            ? rs.getTimestamp("last_order_date").toInstant() 
                            : null,
                    rating
            );
        }, days);
    }

    public SalesAnalyticsDto getSalesAnalytics(Instant fromDate, Instant toDate) {
        // Общая статистика продаж
        String summarySql = """
            SELECT 
                COUNT(DISTINCT o.id) as total_orders,
                COALESCE(SUM(o.total_price), 0) as total_revenue,
                COALESCE(AVG(o.total_price), 0) as avg_order_value,
                COUNT(DISTINCT oi.product_id) as unique_products_sold,
                COALESCE(SUM(oi.quantity), 0) as total_quantity_sold
            FROM outgoing_order o
            JOIN outgoing_order_item oi ON o.id = oi.order_id
            WHERE o.created_at >= ? AND o.created_at <= ?
              AND o.status IN ('SHIPPED'::sales_status, 'RESERVED'::sales_status, 'PICKING'::sales_status)
        """;

        // Используем query вместо queryForObject для обработки случая, когда нет данных
        List<Object[]> summaryList = jdbcTemplate.query(summarySql, (rs, rowNum) -> {
            return new Object[] {
                rs.getLong("total_orders"),
                rs.getBigDecimal("total_revenue"),
                rs.getBigDecimal("avg_order_value"),
                rs.getInt("unique_products_sold"),
                rs.getBigDecimal("total_quantity_sold")
            };
        }, Timestamp.from(fromDate), Timestamp.from(toDate));

        // Если нет данных, возвращаем нулевые значения
        Object[] summary = summaryList.isEmpty() 
            ? new Object[] { 0L, BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO }
            : summaryList.get(0);

        long totalOrders = ((Number) summary[0]).longValue();
        BigDecimal totalRevenue = summary[1] != null ? (BigDecimal) summary[1] : BigDecimal.ZERO;
        BigDecimal avgOrderValue = summary[2] != null ? (BigDecimal) summary[2] : BigDecimal.ZERO;
        int uniqueProducts = ((Number) summary[3]).intValue();
        BigDecimal totalQuantity = summary[4] != null ? (BigDecimal) summary[4] : BigDecimal.ZERO;

        // Топ товары по продажам
        String topProductsSql = """
            SELECT 
                p.id as product_id,
                p.sku,
                p.name,
                SUM(oi.quantity) as total_quantity,
                SUM(oi.quantity * oi.unit_price) as total_revenue
            FROM outgoing_order_item oi
            JOIN outgoing_order o ON oi.order_id = o.id
            JOIN product p ON oi.product_id = p.id
            WHERE o.created_at >= ? AND o.created_at <= ?
              AND o.status IN ('SHIPPED'::sales_status, 'RESERVED'::sales_status, 'PICKING'::sales_status)
            GROUP BY p.id, p.sku, p.name
            ORDER BY total_revenue DESC
            LIMIT 10
        """;

        List<SalesAnalyticsDto.TopProductDto> topProducts = jdbcTemplate.query(topProductsSql, (rs, rowNum) ->
            new SalesAnalyticsDto.TopProductDto(
                rs.getLong("product_id"),
                rs.getString("sku") != null ? rs.getString("sku") : "",
                rs.getString("name") != null ? rs.getString("name") : "",
                rs.getBigDecimal("total_quantity") != null ? rs.getBigDecimal("total_quantity") : BigDecimal.ZERO,
                rs.getBigDecimal("total_revenue") != null ? rs.getBigDecimal("total_revenue") : BigDecimal.ZERO
            ),
            Timestamp.from(fromDate), Timestamp.from(toDate)
        );

        // Продажи по дням (тренд) — статусы и тип как в summarySql / enum sales_status
        String dailyTrendSql = """
            SELECT 
                DATE_TRUNC('day', o.created_at)::date as sale_date,
                COUNT(DISTINCT o.id) as orders_count,
                COALESCE(SUM(o.total_price), 0) as daily_revenue
            FROM outgoing_order o
            WHERE o.created_at >= ? AND o.created_at <= ?
              AND o.status IN ('SHIPPED'::sales_status, 'RESERVED'::sales_status, 'PICKING'::sales_status)
            GROUP BY DATE_TRUNC('day', o.created_at)
            ORDER BY sale_date
            """;

        List<SalesAnalyticsDto.DailySalesDto> dailySales = jdbcTemplate.query(dailyTrendSql, (rs, rowNum) -> {
            java.sql.Timestamp timestamp = rs.getTimestamp("sale_date");
            Instant saleDate = timestamp != null ? timestamp.toInstant() : Instant.now();
            return new SalesAnalyticsDto.DailySalesDto(
                saleDate,
                rs.getLong("orders_count"),
                rs.getBigDecimal("daily_revenue") != null ? rs.getBigDecimal("daily_revenue") : BigDecimal.ZERO
            );
        }, Timestamp.from(fromDate), Timestamp.from(toDate));

        // Продажи по категориям
        String categorySalesSql = """
            SELECT 
                c.id as category_id,
                c.name as category_name,
                COUNT(DISTINCT o.id) as orders_count,
                COALESCE(SUM(oi.quantity * oi.unit_price), 0) as category_revenue
            FROM outgoing_order_item oi
            JOIN outgoing_order o ON oi.order_id = o.id
            JOIN product p ON oi.product_id = p.id
            JOIN product_categories pc ON p.id = pc.product_id
            JOIN categories c ON pc.category_id = c.id
            WHERE o.created_at >= ? AND o.created_at <= ?
              AND o.status IN ('SHIPPED'::sales_status, 'RESERVED'::sales_status, 'PICKING'::sales_status)
            GROUP BY c.id, c.name
            ORDER BY category_revenue DESC
            LIMIT 10
        """;

        List<SalesAnalyticsDto.CategorySalesDto> categorySales = jdbcTemplate.query(categorySalesSql, (rs, rowNum) ->
            new SalesAnalyticsDto.CategorySalesDto(
                rs.getLong("category_id"),
                rs.getString("category_name") != null ? rs.getString("category_name") : "",
                rs.getLong("orders_count"),
                rs.getBigDecimal("category_revenue") != null ? rs.getBigDecimal("category_revenue") : BigDecimal.ZERO
            ),
            Timestamp.from(fromDate), Timestamp.from(toDate)
        );

        return new SalesAnalyticsDto(
            totalRevenue,
            avgOrderValue,
            totalOrders,
            totalQuantity,
            topProducts,
            dailySales,
            categorySales
        );
    }
}

