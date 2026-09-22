package com.kis.wmsapplication.modules.inventoryModule.repository;


import com.kis.wmsapplication.modules.inventoryModule.model.ProductLocationStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockRepository extends JpaRepository<ProductLocationStock, Long> {
    Optional<ProductLocationStock> findByLocationIdAndProductId(Long locationId, Long productId);
    List<ProductLocationStock> findAllByProductId(Long productId);
    
    @Query("SELECT s FROM ProductLocationStock s WHERE s.location.id = :locationId")
    List<ProductLocationStock> findAllByLocationId(@Param("locationId") Long locationId);
}