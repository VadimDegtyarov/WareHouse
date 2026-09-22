package com.kis.wmsapplication.modules.warehouseModule.repository;

import com.kis.wmsapplication.modules.warehouseModule.model.Attribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttributeRepository extends JpaRepository<Attribute, Long> {
}
