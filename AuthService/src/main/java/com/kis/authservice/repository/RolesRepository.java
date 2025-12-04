package com.kis.authservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.kis.authservice.model.Role;

import java.util.Optional;

public interface RolesRepository extends JpaRepository<Role,Integer> {
    Optional<Role> findByRole(String name);

    Role getByRole(String role);
}
