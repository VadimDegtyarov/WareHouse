package com.kis.wmsapplication.modules.inventoryModule.model;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class ProductLocationStockId implements Serializable {
    private Long locationId;
    private Long productId;
}
