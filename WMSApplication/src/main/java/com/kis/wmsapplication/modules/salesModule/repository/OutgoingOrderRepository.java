package com.kis.wmsapplication.modules.salesModule.repository;

import com.kis.wmsapplication.modules.salesModule.enums.SalesStatus;
import com.kis.wmsapplication.modules.salesModule.model.Customer;
import com.kis.wmsapplication.modules.salesModule.model.OutgoingOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface OutgoingOrderRepository extends JpaRepository<OutgoingOrder, Long> {
    List<OutgoingOrder> findByStatus(SalesStatus status);
    
    @Query("SELECT o FROM OutgoingOrder o ORDER BY o.createdAt DESC")
    List<OutgoingOrder> findAllOrderByCreatedAtDesc();
}
