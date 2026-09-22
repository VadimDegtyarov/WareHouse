package com.kis.wmsapplication.modules.dss.analytics;

import com.kis.wmsapplication.modules.dss.dto.ReorderCandidateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StockAnalyticsRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<ReorderCandidateDto> findProductsBelowReorderPoint() {
        String sql = """
            SELECT 
                p.id as product_id,
                p.sku,
                p.supplier_id,
                p.reorder_point,
                p.eoq,
                p.max_stock, -- <--- Добавили в выборку
                COALESCE(SUM(s.quantity), 0) - COALESCE(SUM(s.reserved), 0) as available_stock
            FROM product p
            LEFT JOIN product_location_stock s ON p.id = s.product_id
            WHERE p.active = true 
              AND p.reorder_point IS NOT NULL 
              AND p.supplier_id IS NOT NULL
            GROUP BY p.id
            HAVING (COALESCE(SUM(s.quantity), 0) - COALESCE(SUM(s.reserved), 0)) <= p.reorder_point
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            BigDecimal available = rs.getBigDecimal("available_stock");
            BigDecimal reorderPoint = rs.getBigDecimal("reorder_point");
            BigDecimal eoq = rs.getBigDecimal("eoq");
            BigDecimal maxStock = rs.getBigDecimal("max_stock");
            
            // Обработка null значений
            if (available == null) available = BigDecimal.ZERO;
            if (reorderPoint == null) reorderPoint = BigDecimal.ZERO;
            if (eoq == null) eoq = BigDecimal.ZERO;
            if (maxStock == null) maxStock = BigDecimal.ZERO;
            
            BigDecimal quantityToOrder = reorderPoint.subtract(available);
            if (quantityToOrder.compareTo(BigDecimal.ZERO) < 0) {
                quantityToOrder = BigDecimal.ZERO;
            }

            return new ReorderCandidateDto(
                    rs.getLong("product_id"),
                    rs.getString("sku"),
                    rs.getLong("supplier_id"),
                    available,
                    reorderPoint,
                    eoq,
                    maxStock,
                    quantityToOrder
            );
        });
    }
}