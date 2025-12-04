package com.kis.wmsapplication.modules.inventoryModule.repository;


import com.kis.wmsapplication.modules.inventoryModule.model.ProductLocationStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockRepository extends JpaRepository<ProductLocationStock, UUID> {
    Optional<ProductLocationStock> findByLocationIdAndProductId(UUID locationId, UUID productId);
    List<ProductLocationStock> findAllByProductId(UUID productId);
}