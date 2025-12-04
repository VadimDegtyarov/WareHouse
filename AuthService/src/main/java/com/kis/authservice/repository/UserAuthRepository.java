package com.kis.authservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.kis.authservice.model.UserAuthInfo;

import java.util.Optional;
import java.util.UUID;

public interface UserAuthRepository extends JpaRepository<UserAuthInfo, UUID> {

    Optional<UserAuthInfo> findByEmail(String email);

    Optional<UserAuthInfo> findByPhoneNumber(String phoneNumber);
}
