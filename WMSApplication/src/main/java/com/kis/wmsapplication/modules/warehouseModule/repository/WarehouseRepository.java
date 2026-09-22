package com.kis.wmsapplication.modules.warehouseModule.repository;

import com.kis.wmsapplication.modules.warehouseModule.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    boolean existsByCode(String code);
}