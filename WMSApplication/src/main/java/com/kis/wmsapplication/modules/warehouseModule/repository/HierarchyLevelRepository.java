package com.kis.wmsapplication.modules.warehouseModule.repository;


import com.kis.wmsapplication.modules.warehouseModule.model.HierarchyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HierarchyLevelRepository extends JpaRepository<HierarchyLevel, UUID> {


    List<HierarchyLevel> findByWarehouseIdAndParentIsNull(UUID warehouseId);
}