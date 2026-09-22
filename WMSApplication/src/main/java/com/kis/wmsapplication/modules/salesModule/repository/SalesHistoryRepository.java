package com.kis.wmsapplication.modules.salesModule.repository;

import com.kis.wmsapplication.modules.salesModule.model.SalesHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SalesHistoryRepository extends JpaRepository<SalesHistory, Long> {
    List<SalesHistory> findByProductId(Long productId);
    List<SalesHistory> findByOccurredAtBetween(Instant startDate, Instant endDate);
    List<SalesHistory> findByProductIdAndOccurredAtBetween(Long productId, Instant startDate, Instant endDate);
}
