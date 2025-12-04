
package com.kis.wmsapplication.modules.inventoryModule.model;

import com.kis.wmsapplication.modules.catalogModule.model.Product;
import com.kis.wmsapplication.modules.warehouseModule.model.HierarchyLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product_location_stock",
        uniqueConstraints = @UniqueConstraint(columnNames = {"location_id", "product_id"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ProductLocationStock {

    @EmbeddedId
    private ProductLocationStockId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("locationId")
    @JoinColumn(name = "location_id", nullable = false)
    private HierarchyLevel location;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;


    // СКОЛЬКО лежит (Физический остаток)
    @Column(nullable = false,columnDefinition = "default 0")
    private BigDecimal quantity;

    // СКОЛЬКО зарезервировано под заказы (нельзя трогать)
    @Column(nullable = false,columnDefinition = "default 0")
    private BigDecimal reserved = BigDecimal.ZERO;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.lastUpdated = Instant.now();
    }
}
