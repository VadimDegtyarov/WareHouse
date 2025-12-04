package com.kis.wmsapplication.modules.inventoryModule.enums;

public enum MovementType {
    RECEIPT,        // Приемка от поставщика
    SHIPMENT,       // Отгрузка клиенту
    TRANSFER,       // Внутреннее перемещение (с полки на полку)
    ADJUSTMENT,     // Инвентаризация (корректировка при утере/находке)
    RETURN          // Возврат от клиента
}