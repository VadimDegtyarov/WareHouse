package com.kis.wmsapplication.modules.procurementModule.repository;

import com.kis.wmsapplication.modules.procurementModule.model.IncomingOrderItem;
import com.kis.wmsapplication.modules.procurementModule.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SupplierRepository extends JpaRepository< Supplier,UUID> {
}
