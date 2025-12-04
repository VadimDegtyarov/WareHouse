package com.kis.wmsapplication.modules.warehouseModule.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "warehouse_hierarchy_level_category")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class HierarchyLevelCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String levelName;
}