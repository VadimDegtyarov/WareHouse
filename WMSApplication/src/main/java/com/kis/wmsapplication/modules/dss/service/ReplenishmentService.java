package com.kis.wmsapplication.modules.dss.service;

import com.kis.wmsapplication.modules.dss.analytics.StockAnalyticsRepository;
import com.kis.wmsapplication.modules.dss.dto.ReorderCandidateDto;
import com.kis.wmsapplication.modules.procurementModule.dto.CreateOrderRequest; // Исправил пакет на procurement
import com.kis.wmsapplication.modules.procurementModule.service.ProcurementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReplenishmentService {

    private final StockAnalyticsRepository analyticsRepository;
    private final ProcurementService procurementService;

    @Transactional
    public void runAnalysisAndCreateOrders() {
        log.info("DSS: Запуск анализа потребностей...");

        List<ReorderCandidateDto> candidates = analyticsRepository.findProductsBelowReorderPoint();

        if (candidates.isEmpty()) {
            log.info("DSS: Дефицита не обнаружено.");
            return;
        }

        Map<UUID, List<ReorderCandidateDto>> bySupplier = candidates.stream()
                .collect(Collectors.groupingBy(ReorderCandidateDto::supplierId));

        bySupplier.forEach(this::createOrderForSupplier);

        log.info("DSS: Анализ завершен. Создано заказов для {} поставщиков.", bySupplier.size());
    }

    private void createOrderForSupplier(UUID supplierId, List<ReorderCandidateDto> items) {
        List<CreateOrderRequest.OrderItemDto> orderItems = new ArrayList<>();

        for (ReorderCandidateDto item : items) {
            BigDecimal qtyToOrder = calculateOrderQuantity(item);

            if (qtyToOrder.compareTo(BigDecimal.ZERO) > 0) {
                orderItems.add(new CreateOrderRequest.OrderItemDto(
                        item.productId(),
                        qtyToOrder,
                        null
                ));

                log.info("DSS: Товар {} (Остаток: {}, Max: {}, EOQ: {}) -> Заказать: {}",
                        item.sku(), item.currentAvailable(), item.maxStock(), item.eoq(), qtyToOrder);
            }
        }

        if (!orderItems.isEmpty()) {
            CreateOrderRequest request = new CreateOrderRequest(supplierId, orderItems);
            UUID orderId = procurementService.createOrder(request);
            log.info("DSS: Создан черновик заказа {} для поставщика {}", orderId, supplierId);
        }
    }

    /**
     * Расчет количества к заказу.
     * Стратегия: Min-Max с учетом EOQ.
     */
    private BigDecimal calculateOrderQuantity(ReorderCandidateDto item) {
        BigDecimal current = item.currentAvailable();
        BigDecimal eoq = item.eoq() != null ? item.eoq() : BigDecimal.ONE;


        BigDecimal targetLevel = item.maxStock() != null
                ? item.maxStock()
                : item.reorderPoint().add(eoq);

        BigDecimal deficitToTarget = targetLevel.subtract(current);

        BigDecimal finalQty = deficitToTarget.max(eoq);


        return finalQty.setScale(3, RoundingMode.CEILING);
    }
}