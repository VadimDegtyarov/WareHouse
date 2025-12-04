package com.kis.wmsapplication.modules.salesModule.model;



import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "customer")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String email;
    private String phone;
    @Column(name = "address_id")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Location addressId;
    //1 - Обычный, 10 - VIP (отгружаем в первую очередь)
    @Column(nullable = false)
    private Integer priority = 1;
}