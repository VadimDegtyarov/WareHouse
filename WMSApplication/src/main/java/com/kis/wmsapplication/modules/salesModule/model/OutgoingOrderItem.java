package com.kis.wmsapplication.modules.salesModule.model;


import com.kis.wmsapplication.modules.catalogModule.model.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "outgoing_order_item")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class OutgoingOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OutgoingOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private BigDecimal quantity; // Сколько хочет клиент

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice; // Цена продажи (фиксируем)

    @Column(name = "reserved_quantity")
    private BigDecimal reservedQuantity = BigDecimal.ZERO; // Сколько реально отложили
}