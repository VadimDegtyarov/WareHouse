package com.kis.wmsapplication.modules.inventoryModule.repository;

import com.kis.wmsapplication.modules.inventoryModule.model.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MovementRepository extends JpaRepository<InventoryMovement, UUID> {
}