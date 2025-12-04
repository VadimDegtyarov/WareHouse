package com.kis.wmsapplication.modules.warehouseModule.repository;

import com.kis.wmsapplication.modules.warehouseModule.model.HierarchyLevelCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HierarchyLevelCategoryRepository extends JpaRepository<HierarchyLevelCategory, Integer> {
    // Этот метод мы используем в сервисе для поиска типа ячейки
    Optional<HierarchyLevelCategory> findByLevelName(String levelName);
}