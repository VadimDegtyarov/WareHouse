package com.kis.wmsapplication.modules.salesModule.repository;

import com.kis.wmsapplication.modules.salesModule.model.Customer;
import com.kis.wmsapplication.modules.salesModule.model.OutgoingOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutgoingOrderRepository extends JpaRepository<OutgoingOrder, Long> {
}
