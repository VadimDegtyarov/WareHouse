package com.kis.wmsapplication.modules.salesModule.model;



import com.kis.wmsapplication.modules.warehouseModule.model.Location;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "customer")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String email;
    private String phone;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")

    private Location addressId;
    //1 - Обычный, 10 - VIP (отгружаем в первую очередь)
    @Column(nullable = false)
    private Integer priority = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    public Long getAddressId(){
        if(this.addressId==null){
            return null;
        }
        return this.addressId.getId();
    }
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}