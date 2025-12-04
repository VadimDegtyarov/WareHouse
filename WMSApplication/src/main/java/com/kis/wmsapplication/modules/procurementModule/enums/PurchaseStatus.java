package com.kis.wmsapplication.modules.procurementModule.enums;


public enum PurchaseStatus {
    PENDING,        // Черновик (создан автоматически или вручную)
    PLACED,         // Отправлен поставщику (ждем подтверждения)
    IN_TRANSIT,     // В пути (поставщик отгрузил)
    RECEIVED,       // Принят на склад (сток увеличен)
    CANCELLED       // Отменен
}