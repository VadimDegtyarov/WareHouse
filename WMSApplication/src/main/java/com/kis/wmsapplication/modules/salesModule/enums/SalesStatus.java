package com.kis.wmsapplication.modules.salesModule.enums;


public enum SalesStatus {
    NEW,            // Заказ создан, но сток еще не проверен
    RESERVED,       // Товар успешно зарезервирован на складе (Hard Lock)
    PICKING,        // Кладовщик собирает заказ
    SHIPPED,        // Товар уехал (Списание со склада)
    CANCELLED       // Отмена (Резервы сняты)
}