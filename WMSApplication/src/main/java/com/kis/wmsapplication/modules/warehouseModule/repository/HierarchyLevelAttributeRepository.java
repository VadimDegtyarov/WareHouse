package com.kis.wmsapplication.modules.warehouseModule.repository;

import com.kis.wmsapplication.modules.warehouseModule.model.HierarchyLevelAttribute;
import com.kis.wmsapplication.modules.warehouseModule.model.HierarchyLevelAttributeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HierarchyLevelAttributeRepository extends JpaRepository<HierarchyLevelAttribute, HierarchyLevelAttributeId> {
    List<HierarchyLevelAttribute> findByLevelId(Long levelId);
    List<HierarchyLevelAttribute> findByAttributeId(Long attributeId);
}
