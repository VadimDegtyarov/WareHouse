package com.kis.wmsapplication.modules.userModule.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.UUID;

@Data
@Entity
@Table(name = "user_info")
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "username")
    private String username;
    @JsonBackReference
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserAuthInfo userAuthInfo;
    @Column(name="first_name")
    private String firstName;
    @Column(name="last_name")
    private String lastName;
    @Column(name="birth_date")
    private Instant birthDate;
    @Column(name = "avatar_url")
    private String avatarURL;
    public void setBirthDate(@NotNull Instant localDate) {
        birthDate = localDate;
    }
    public void setBirthDate(@NotNull LocalDate localDate) {
        birthDate = localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
