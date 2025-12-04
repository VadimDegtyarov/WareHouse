package com.kis.wmsapplication.modules.catalogModule.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "product")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sku", nullable = false, unique = true)
    private String sku;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price", nullable = false)
    private BigDecimal price;


    @Column(name = "max_stock")
    private BigDecimal maxStock; // Максимальный уровень запаса (емкость ячейки или стратегия)
    @Column(name = "min_stock")
    private BigDecimal minStock; // Минимальный остаток

    @Column(name = "reorder_point")
    private BigDecimal reorderPoint; // Точка перезаказа

    @Column(name = "eoq")
    private BigDecimal eoq; //сколько заказывать

    @Column(name = "active", nullable = false)
    private Boolean active = true;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_categories",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    // В будущем тут нужна связь с Supplier, пока ID
    @Column(name = "supplier_id")
    private UUID supplierId;
}