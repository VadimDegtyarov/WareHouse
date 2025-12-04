package com.kis.wmsapplication.modules.warehouseModule.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "location")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String country;
    private String city;
    private String street;
    private String house;
    @Column(name = "postal_code")
    private String postalCode;

    // Координаты для карты
    private Double latitude;
    private Double longitude;
}