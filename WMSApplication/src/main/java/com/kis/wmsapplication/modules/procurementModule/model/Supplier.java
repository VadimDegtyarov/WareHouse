package com.kis.wmsapplication.modules.procurementModule.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "supplier")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;
    @Column(name = "contact_email")
    private String contactEmail;
    private String phone;

    // Срок поставки в днях (для формул СППР)
    @Column(name = "avg_lead_time_days", nullable = false)
    private Integer avgLeadTimeDays = 7;
}