package org.com.pravin.inventoryservice.repository;

import org.com.pravin.inventoryservice.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    boolean existsByOrderId(Long orderId);
    List<StockReservation> findByOrderId(Long orderId);
}
