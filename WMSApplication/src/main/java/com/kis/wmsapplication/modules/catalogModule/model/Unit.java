package com.kis.wmsapplication.modules.catalogModule.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;
import java.util.Set;

@Entity
@Table(name = "units")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;



    @Column(name = "code", nullable = false, length = 10, unique = true)
    private String code;
    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "unit", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Product> products;
}