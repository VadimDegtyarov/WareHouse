package com.kis.wmsapplication.modules.dss.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис прогнозирования спроса и расчёта параметров управления запасами (DSS).
 *
 * Реализует:
 *  - Прогнозирование спроса методом скользящего среднего по истории продаж
 *  - EOQ (Economic Order Quantity) — формула Уилсона
 *  - ROP (Reorder Point) — точка перезаказа
 *  - Safety Stock — страховой запас
 *  - Расчёт всех параметров одной операцией
 *  - Среднедневной спрос на основе фактических отгрузок
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ForecastService {

    private final JdbcTemplate jdbcTemplate;

    /** Стандартное отклонение нормального распределения для уровня сервиса 95% */
    private static final BigDecimal SERVICE_LEVEL_Z = new BigDecimal("1.65");

    /** Стоимость размещения одного заказа (условная), руб. */
    private static final BigDecimal ORDER_COST = new BigDecimal("500");

    /** Доля затрат на хранение от стоимости товара в год (20%) */
    private static final BigDecimal HOLDING_COST_RATE = new BigDecimal("0.20");

    private static final int FORECAST_HORIZON_DAYS = 30;

    /**
     * Прогнозирование спроса методом скользящего среднего.
     * Берём среднюю отгрузку за последние 90 дней и масштабируем на горизонт прогноза.
     */
    public BigDecimal forecastDemand(Long productId, int days) {
        BigDecimal dailyDemand = getAverageDailyDemand(productId, 90);
        BigDecimal forecast = dailyDemand.multiply(BigDecimal.valueOf(days));
        log.debug("Прогноз спроса productId={} на {} дней = {} (среднедневной {})", productId, days, forecast, dailyDemand);
        return forecast.setScale(2, RoundingMode.HALF_UP);
    }

    /** Источник данных для расчёта спроса. */
    public enum DemandSource { SALES, RECEIPTS, MOVEMENTS, NONE }

    /**
     * Средний дневной спрос (фактический) по истории отгрузок за период.
     * Если данных нет — возвращает 0.
     */
    public BigDecimal getAverageDailyDemand(Long productId, int historicalDays) {
        return getDemandWithSource(productId, historicalDays).demand;
    }

    /** Контейнер: дневной спрос + источник данных, использованный для расчёта. */
    public static class DemandResult {
        public final BigDecimal demand;
        public final DemandSource source;
        public DemandResult(BigDecimal demand, DemandSource source) {
            this.demand = demand == null ? BigDecimal.ZERO : demand;
            this.source = source == null ? DemandSource.NONE : source;
        }
    }

    /**
     * Получить средний дневной спрос с информацией об источнике.
     * Логика fallback:
     *  1) Если есть отгрузки за период (outgoing_order) — берём их.
     *  2) Если отгрузок нет, но есть движения SHIPMENT (inventory_movement) — берём их.
     *  3) Если и тех нет, используем приёмки (incoming_order) как косвенный сигнал оборачиваемости (50% от среднего поступления).
     *  4) Иначе возвращаем 0 c источником NONE.
     */
    public DemandResult getDemandWithSource(Long productId, int historicalDays) {
        if (productId == null || historicalDays <= 0) {
            return new DemandResult(BigDecimal.ZERO, DemandSource.NONE);
        }
        BigDecimal sales = querySalesDailyDemand(productId, historicalDays);
        if (sales.compareTo(BigDecimal.ZERO) > 0) {
            return new DemandResult(sales, DemandSource.SALES);
        }
        BigDecimal movements = queryMovementDailyDemand(productId, historicalDays);
        if (movements.compareTo(BigDecimal.ZERO) > 0) {
            return new DemandResult(movements, DemandSource.MOVEMENTS);
        }
        BigDecimal receipts = queryReceiptDailyDemand(productId, historicalDays);
        if (receipts.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal proxy = receipts.multiply(new BigDecimal("0.5")).setScale(4, RoundingMode.HALF_UP);
            return new DemandResult(proxy, DemandSource.RECEIPTS);
        }
        return new DemandResult(BigDecimal.ZERO, DemandSource.NONE);
    }

    /**
     * Фактический среднедневной спрос — считается только по реально отгруженным заказам (status = SHIPPED).
     * Резервы (RESERVED) и комплектация (PICKING) намеренно исключены: это ещё не отгрузка, и их попадание
     * в выборку искусственно завышало бы статистику и СКО.
     *
     * Если у заказа заполнен {@code shipped_at} (фактическая дата отгрузки) — используем его,
     * иначе откатываемся на {@code created_at}.
     */
    private BigDecimal querySalesDailyDemand(Long productId, int historicalDays) {
        String sql = """
            SELECT COALESCE(SUM(oi.quantity), 0)::numeric / ? AS daily_demand
            FROM outgoing_order_item oi
            JOIN outgoing_order o ON oi.order_id = o.id
            WHERE oi.product_id = ?
              AND o.status = 'SHIPPED'::sales_status
              AND COALESCE(o.shipped_at, o.created_at) >= NOW() - (? * INTERVAL '1 day')
            """;
        try {
            BigDecimal v = jdbcTemplate.queryForObject(sql, BigDecimal.class, historicalDays, productId, historicalDays);
            return v == null ? BigDecimal.ZERO : v.setScale(4, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            // Если колонки shipped_at нет — повторяем запрос без неё, чтобы не падать на старой БД.
            log.warn("Sales demand SQL failed for productId={} ({}); fallback to created_at only", productId, ex.getMessage());
            String fallback = """
                SELECT COALESCE(SUM(oi.quantity), 0)::numeric / ? AS daily_demand
                FROM outgoing_order_item oi
                JOIN outgoing_order o ON oi.order_id = o.id
                WHERE oi.product_id = ?
                  AND o.status = 'SHIPPED'::sales_status
                  AND o.created_at >= NOW() - (? * INTERVAL '1 day')
                """;
            try {
                BigDecimal v = jdbcTemplate.queryForObject(fallback, BigDecimal.class, historicalDays, productId, historicalDays);
                return v == null ? BigDecimal.ZERO : v.setScale(4, RoundingMode.HALF_UP);
            } catch (Exception ex2) {
                return BigDecimal.ZERO;
            }
        }
    }

    private BigDecimal queryMovementDailyDemand(Long productId, int historicalDays) {
        String sql = """
            SELECT COALESCE(SUM(m.quantity), 0)::numeric / ? AS daily_demand
            FROM inventory_movement m
            WHERE m.product_id = ?
              AND m.movement_type_id = (SELECT id FROM inventory_movement_type WHERE code = 'SHIPMENT' LIMIT 1)
              AND m.movement_date >= NOW() - (? * INTERVAL '1 day')
            """;
        try {
            BigDecimal v = jdbcTemplate.queryForObject(sql, BigDecimal.class, historicalDays, productId, historicalDays);
            return v == null ? BigDecimal.ZERO : v.setScale(4, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal queryReceiptDailyDemand(Long productId, int historicalDays) {
        String sql = """
            SELECT COALESCE(SUM(ii.quantity), 0)::numeric / ? AS daily_demand
            FROM incoming_order_item ii
            JOIN incoming_order io ON ii.order_id = io.id
            WHERE ii.product_id = ?
              AND io.order_date >= NOW() - (? * INTERVAL '1 day')
            """;
        try {
            BigDecimal v = jdbcTemplate.queryForObject(sql, BigDecimal.class, historicalDays, productId, historicalDays);
            return v == null ? BigDecimal.ZERO : v.setScale(4, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Среднеквадратичное отклонение дневного спроса (для страхового запаса).
     * Считается только по фактическим отгрузкам (status = SHIPPED), чтобы СКО отражало реальную
     * волатильность спроса, а не «шум» зарезервированных, но не отгруженных заказов.
     */
    public BigDecimal getDemandStdDev(Long productId, int historicalDays) {
        if (productId == null || historicalDays <= 0) {
            return BigDecimal.ZERO;
        }
        String sql = """
            WITH days AS (
                SELECT generate_series(
                    (NOW() - (? * INTERVAL '1 day'))::date,
                    NOW()::date,
                    '1 day'::interval
                )::date AS d
            ),
            daily AS (
                SELECT d AS day, COALESCE(SUM(oi.quantity), 0)::numeric AS qty
                FROM days
                LEFT JOIN outgoing_order o ON DATE(COALESCE(o.shipped_at, o.created_at)) = days.d
                    AND o.status = 'SHIPPED'::sales_status
                LEFT JOIN outgoing_order_item oi ON oi.order_id = o.id AND oi.product_id = ?
                GROUP BY d
            )
            SELECT COALESCE(STDDEV_POP(qty), 0) FROM daily;
            """;
        try {
            BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class,
                    historicalDays, productId);
            return value == null ? BigDecimal.ZERO : value.setScale(4, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            // Если поля shipped_at нет — повторяем без него.
            log.warn("StdDev demand SQL failed for productId={} ({}); fallback to created_at only", productId, ex.getMessage());
            String fallback = """
                WITH days AS (
                    SELECT generate_series(
                        (NOW() - (? * INTERVAL '1 day'))::date,
                        NOW()::date,
                        '1 day'::interval
                    )::date AS d
                ),
                daily AS (
                    SELECT d AS day, COALESCE(SUM(oi.quantity), 0)::numeric AS qty
                    FROM days
                    LEFT JOIN outgoing_order o ON DATE(o.created_at) = days.d
                        AND o.status = 'SHIPPED'::sales_status
                    LEFT JOIN outgoing_order_item oi ON oi.order_id = o.id AND oi.product_id = ?
                    GROUP BY d
                )
                SELECT COALESCE(STDDEV_POP(qty), 0) FROM daily;
                """;
            try {
                BigDecimal value = jdbcTemplate.queryForObject(fallback, BigDecimal.class,
                        historicalDays, productId);
                return value == null ? BigDecimal.ZERO : value.setScale(4, RoundingMode.HALF_UP);
            } catch (Exception ex2) {
                return BigDecimal.ZERO;
            }
        }
    }

    /**
     * Получить срок поставки в днях у поставщика товара.
     */
    private int getLeadTimeDays(Long productId, int defaultLeadTime) {
        if (productId == null) return defaultLeadTime;
        String sql = """
            SELECT COALESCE(s.avg_lead_time_days, ?) AS lead_time
            FROM product p
            LEFT JOIN supplier s ON p.supplier_id = s.id
            WHERE p.id = ?
            """;
        try {
            Integer value = jdbcTemplate.queryForObject(sql, Integer.class, defaultLeadTime, productId);
            return value == null ? defaultLeadTime : value;
        } catch (Exception ex) {
            return defaultLeadTime;
        }
    }

    /**
     * Получить цену товара (для EOQ).
     */
    private BigDecimal getProductPrice(Long productId) {
        String sql = "SELECT COALESCE(unit_price, 1) FROM product WHERE id = ?";
        try {
            BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, productId);
            return value == null ? BigDecimal.ONE : value;
        } catch (Exception ex) {
            return BigDecimal.ONE;
        }
    }

    /**
     * Расчёт страхового запаса:
     *   SS = Z * σ_d * √L
     * где Z — коэффициент уровня сервиса, σ_d — СКО дневного спроса, L — срок поставки.
     */
    public BigDecimal calculateSafetyStock(Long productId, int leadTimeDays) {
        BigDecimal stdDev = getDemandStdDev(productId, 90);
        if (stdDev.compareTo(BigDecimal.ZERO) <= 0) {
            // Если данных нет, используем эмпирическую формулу: SS = 20% от прогнозируемого спроса
            BigDecimal forecast = forecastDemand(productId, leadTimeDays);
            return forecast.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
        }
        double sqrtLeadTime = Math.sqrt(Math.max(leadTimeDays, 1));
        BigDecimal safetyStock = SERVICE_LEVEL_Z
                .multiply(stdDev)
                .multiply(BigDecimal.valueOf(sqrtLeadTime));
        return safetyStock.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Расчёт точки перезаказа (Reorder Point):
     *   ROP = d * L + SS
     * где d — средний дневной спрос, L — срок поставки, SS — страховой запас.
     */
    public BigDecimal calculateReorderPoint(Long productId) {
        int leadTime = getLeadTimeDays(productId, 7);
        BigDecimal dailyDemand = getAverageDailyDemand(productId, 90);
        BigDecimal safetyStock = calculateSafetyStock(productId, leadTime);
        BigDecimal rop = dailyDemand
                .multiply(BigDecimal.valueOf(leadTime))
                .add(safetyStock);
        return rop.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Расчёт оптимального размера заказа (формула Уилсона):
     *   EOQ = √( 2 * D * S / H )
     * где D — годовой спрос, S — стоимость заказа, H — стоимость хранения единицы товара в год.
     */
    public BigDecimal calculateEOQ(Long productId) {
        DemandResult dr = getDemandWithSource(productId, 90);
        BigDecimal dailyDemand = dr.demand;
        BigDecimal annualDemand = dailyDemand.multiply(BigDecimal.valueOf(365));
        if (annualDemand.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal unitPrice = getProductPrice(productId);
        BigDecimal holdingCost = unitPrice.multiply(HOLDING_COST_RATE);
        if (holdingCost.compareTo(BigDecimal.ZERO) <= 0) {
            holdingCost = new BigDecimal("0.1");
        }
        double eoq = Math.sqrt(2.0
                * annualDemand.doubleValue()
                * ORDER_COST.doubleValue()
                / holdingCost.doubleValue());
        return BigDecimal.valueOf(eoq).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Расчёт всех параметров управления запасами одной операцией.
     * Дополнительно возвращает источник данных (sales / movements / receipts / none),
     * признак наличия истории и текстовое описание источника — чтобы клиент мог корректно
     * объяснить пользователю, почему значения могут быть низкими или нулевыми.
     */
    public Map<String, Object> calculateInventoryParameters(Long productId) {
        int leadTime = getLeadTimeDays(productId, 7);
        DemandResult dr = getDemandWithSource(productId, 90);
        BigDecimal avgDailyDemand = dr.demand;
        BigDecimal safetyStock = calculateSafetyStock(productId, leadTime);
        BigDecimal reorderPoint = calculateReorderPoint(productId);
        BigDecimal eoq = calculateEOQ(productId);
        BigDecimal minStock = safetyStock;
        BigDecimal maxStock = reorderPoint.add(eoq);

        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("eoq", eoq);
        result.put("reorderPoint", reorderPoint);
        result.put("safetyStock", safetyStock);
        result.put("minStock", minStock);
        result.put("maxStock", maxStock);
        result.put("avgDailyDemand", avgDailyDemand);
        result.put("leadTimeDays", leadTime);
        result.put("demandSource", dr.source.name());
        result.put("hasHistory", dr.source != DemandSource.NONE);
        result.put("dataDescription", describeSource(dr.source));
        return result;
    }

    /** Расчёт EOQ с учётом возможного fallback по источнику данных. */
    public BigDecimal calculateEOQWithSource(Long productId) {
        DemandResult dr = getDemandWithSource(productId, 90);
        if (dr.demand.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        BigDecimal annualDemand = dr.demand.multiply(BigDecimal.valueOf(365));
        BigDecimal unitPrice = getProductPrice(productId);
        BigDecimal holdingCost = unitPrice.multiply(HOLDING_COST_RATE);
        if (holdingCost.compareTo(BigDecimal.ZERO) <= 0) holdingCost = new BigDecimal("0.1");
        double eoq = Math.sqrt(2.0 * annualDemand.doubleValue() * ORDER_COST.doubleValue() / holdingCost.doubleValue());
        return BigDecimal.valueOf(eoq).setScale(2, RoundingMode.HALF_UP);
    }

    private String describeSource(DemandSource source) {
        return switch (source) {
            case SALES -> "Данные на основе истории продаж за 90 дней";
            case MOVEMENTS -> "Данные на основе движений товара (отгрузки) за 90 дней";
            case RECEIPTS -> "Данных продаж нет, используется оценка на основе истории поступлений (50% от приходов)";
            case NONE -> "Недостаточно данных для прогноза. Необходима история отгрузок, движений или приёмок.";
        };
    }

    /**
     * Расчёт количества к дозаказу по стратегии Min-Max c учётом EOQ:
     *   suggestedOrder = max( (ROP + EOQ) - currentStock , 0 )
     *
     * Возвращает 0, если текущий остаток уже достиг или превышает целевой уровень (ROP + EOQ).
     * Это согласуется с алгоритмом {@link ReplenishmentService#runAnalysisAndCreateOrders()}
     * и не приводит к двойному учёту страхового запаса (SS уже включён в ROP).
     */
    public BigDecimal calculateOptimalStockLevel(Long productId, BigDecimal currentStock, int leadTimeDays) {
        BigDecimal rop = calculateReorderPoint(productId);
        BigDecimal eoq = calculateEOQ(productId);
        BigDecimal target = rop.add(eoq);
        BigDecimal stock = currentStock == null ? BigDecimal.ZERO : currentStock;
        BigDecimal need = target.subtract(stock);
        if (need.compareTo(BigDecimal.ZERO) < 0) {
            need = BigDecimal.ZERO;
        }
        return need.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Текстовые рекомендации по оптимизации запасов.
     */
    public List<String> getOptimizationRecommendations(Long productId, BigDecimal currentStock, BigDecimal optimalLevel) {
        List<String> recommendations = new ArrayList<>();
        if (optimalLevel == null) optimalLevel = BigDecimal.ZERO;
        if (currentStock == null) currentStock = BigDecimal.ZERO;

        BigDecimal upperBound = optimalLevel.multiply(new BigDecimal("1.5"));
        BigDecimal lowerBound = optimalLevel.multiply(new BigDecimal("0.5"));

        if (currentStock.compareTo(upperBound) > 0) {
            recommendations.add("Избыточный запас. Рекомендуется снизить закупки и провести распродажу.");
        } else if (currentStock.compareTo(lowerBound) < 0) {
            recommendations.add("Недостаточный запас. Рекомендуется срочно увеличить закупки.");
        } else {
            recommendations.add("Уровень запасов оптимален. Поддерживайте текущую стратегию.");
        }

        BigDecimal eoq = calculateEOQ(productId);
        if (eoq.compareTo(BigDecimal.ZERO) > 0) {
            recommendations.add("Рекомендуемый размер заказа (EOQ): " + eoq.toPlainString() + " ед.");
        }

        BigDecimal rop = calculateReorderPoint(productId);
        if (rop.compareTo(BigDecimal.ZERO) > 0) {
            recommendations.add("Точка перезаказа (ROP): " + rop.toPlainString() + " ед.");
        }
        return recommendations;
    }
}
