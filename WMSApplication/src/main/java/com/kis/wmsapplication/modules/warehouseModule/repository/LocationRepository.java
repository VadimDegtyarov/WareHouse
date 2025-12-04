package com.kis.wmsapplication.modules.warehouseModule.repository;


import com.kis.wmsapplication.modules.warehouseModule.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {
}