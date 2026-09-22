package com.kis.wmsapplication.modules.inventoryModule.repository;

import com.kis.wmsapplication.modules.inventoryModule.model.InventoryMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryMovementTypeRepository extends JpaRepository<InventoryMovementType, Integer> {
    Optional<InventoryMovementType> findByType(String type);
}
