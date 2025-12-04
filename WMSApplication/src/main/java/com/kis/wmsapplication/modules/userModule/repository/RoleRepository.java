package com.kis.wmsapplication.modules.userModule.repository;

import com.kis.wmsapplication.modules.userModule.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role,Integer> {
    Optional<Role> findByRole(String role);
}
