package com.kis.wmsapplication.modules.warehouseModule.model;



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
@Table(name = "warehouse_hierarchy_level",
        uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "code"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class HierarchyLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private HierarchyLevel parent;

    // Дети (например, Полки внутри Стеллажа)
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HierarchyLevel> children = new ArrayList<>();


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "level_id")
    private HierarchyLevelCategory category;

    @Column(nullable = false)
    private String code; // Человекочитаемый код: "A-01-02"

    private String name;

    private BigDecimal capacity; // Вместимость конкретной ячейки

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // Вспомогательный метод для добавления доченрних елементов
    public void addChild(HierarchyLevel child) {
        children.add(child);
        child.setParent(this);
        child.setWarehouse(this.warehouse); // Ребенок всегда принадлежит тому же складу
    }
}