package com.kis.wmsapplication.modules.salesModule.model;


import com.kis.wmsapplication.modules.salesModule.enums.SalesStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "outgoing_order")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class OutgoingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SalesStatus status = SalesStatus.NEW;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OutgoingOrderItem> items = new ArrayList<>();

    public void addItem(OutgoingOrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}