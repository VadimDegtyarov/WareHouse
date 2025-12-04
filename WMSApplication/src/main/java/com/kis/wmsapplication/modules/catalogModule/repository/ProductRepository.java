package com.kis.wmsapplication.modules.catalogModule.repository;


import com.kis.wmsapplication.modules.catalogModule.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {


    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);
}