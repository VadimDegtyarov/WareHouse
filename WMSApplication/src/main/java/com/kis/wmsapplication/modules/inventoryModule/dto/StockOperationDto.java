package com.kis.wmsapplication.modules.inventoryModule.dto;



import com.kis.wmsapplication.modules.inventoryModule.enums.MovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record StockOperationDto(
        @NotNull UUID productId,
        UUID fromLocationId, // Откуда (для перемещения)
        UUID toLocationId,   // Куда (для приемки/перемещения)
        @NotNull @Positive BigDecimal quantity,
        MovementType type,   // RECEIPT, TRANSFER...
        String reference     // Комментарий (№ накладной)
) {}