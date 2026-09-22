package com.kis.wmsapplication.modules.procurementModule.model;

import com.kis.wmsapplication.modules.procurementModule.enums.CompanyRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "supplier")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    @Column(name = "contact_email")
    private String contactEmail;
    private String phone;

    // Срок поставки в днях (для формул СППР)
    @Column(name = "avg_lead_time_days", nullable = false)
    private Integer avgLeadTimeDays = 7;

    // Роль компании: поставщик, покупатель или оба
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) // PostgreSQL enum supplier_roles
    @Column(name = "company_role", nullable = false, columnDefinition = "supplier_roles")
    private CompanyRole companyRole = CompanyRole.SUPPLIER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}