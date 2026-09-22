package com.kis.wmsapplication.modules.procurementModule.repository;

import com.kis.wmsapplication.modules.procurementModule.enums.PurchaseStatus;
import com.kis.wmsapplication.modules.procurementModule.model.IncomingOrder;
import com.kis.wmsapplication.modules.procurementModule.model.IncomingOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface IncomingOrderRepository extends JpaRepository<IncomingOrder,Long> {
    List<IncomingOrder> findByStatus(PurchaseStatus status);
    
    @Query("SELECT o FROM IncomingOrder o ORDER BY o.orderDate DESC")
    List<IncomingOrder> findAllOrderByOrderDateDesc();
}
