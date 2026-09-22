package com.kis.wmsapplication.modules.dss.dto;

import java.math.BigDecimal;
import java.time.Instant;
public record MovementHistoryDto(
        Long id,
        Long productId,
        String productSku,
        String productName,
        BigDecimal quantity,
        String fromLocationCode,
        String fromLocationName,
        String toLocationCode,
        String toLocationName,
        String movementType,
        Instant occurredAt,
        String reference
) {}

