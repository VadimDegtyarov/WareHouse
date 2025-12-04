package com.kis.authservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.kis.authservice.model.DeactivatedToken;

import java.util.UUID;

public interface DeactivatedTokenRepository extends JpaRepository<DeactivatedToken, UUID> {
}
