package com.kis.wmsapplication.modules.inventoryModule.model;

import com.kis.wmsapplication.modules.catalogModule.model.Product;
import com.kis.wmsapplication.modules.inventoryModule.enums.MovementType;
import com.kis.wmsapplication.modules.warehouseModule.model.HierarchyLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_movement")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private BigDecimal quantity; // Сколько переместили

    // Откуда взяли (NULL если это Приемка извне)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_location_id")
    private HierarchyLevel fromLocation;

    // Куда положили (NULL если это Списание/Продажа)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_location_id")
    private HierarchyLevel toLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private MovementType type;

    @Column(name = "occurred_at")
    private Instant occurredAt = Instant.now();

    private String reference; // Номер накладной, заказа или комментарий
}