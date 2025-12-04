package com.kis.wmsapplication.modules.procurementModule.model;


import com.kis.wmsapplication.modules.catalogModule.model.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "incoming_order_item")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class IncomingOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private IncomingOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "purchase_price")
    private BigDecimal purchasePrice; // Цена закупки (может отличаться от каталожной)
}