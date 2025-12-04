package com.kis.wmsapplication.modules.procurementModule.model;


import com.kis.wmsapplication.modules.procurementModule.enums.PurchaseStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "incoming_order")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class IncomingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseStatus status = PurchaseStatus.PENDING;

    @Column(name = "order_date")
    private Instant orderDate = Instant.now();

    @Column(name = "expected_arrival")
    private Instant expectedArrival;

    @Column(name = "actual_arrival")
    private Instant actualArrival;

    // Состав заказа (Items)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IncomingOrderItem> items = new ArrayList<>();

    // Вспомогательный метод
    public void addItem(IncomingOrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}