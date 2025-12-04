package com.kis.wmsapplication.modules.salesModule.repository;

import com.kis.wmsapplication.modules.salesModule.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
}
