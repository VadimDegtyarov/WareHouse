package com.kis.authservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.kis.authservice.model.User;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
