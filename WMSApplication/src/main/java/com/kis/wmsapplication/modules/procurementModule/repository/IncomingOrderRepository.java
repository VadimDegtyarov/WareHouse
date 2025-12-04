package com.kis.wmsapplication.modules.procurementModule.repository;

import com.kis.wmsapplication.modules.procurementModule.model.IncomingOrder;
import com.kis.wmsapplication.modules.procurementModule.model.IncomingOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IncomingOrderRepository extends JpaRepository<IncomingOrder,UUID> {
}
