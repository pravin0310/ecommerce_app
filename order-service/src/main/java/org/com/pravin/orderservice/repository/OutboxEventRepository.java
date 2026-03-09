package org.com.pravin.orderservice.repository;

import org.com.pravin.orderservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    // Find events waiting to be published
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(
            OutboxEvent.OutboxStatus status);

    // Find pending events with retry limit
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'PENDING'" +
            " AND o.retryCount < :maxRetries" +
            " ORDER BY o.createdAt ASC")
    List<OutboxEvent> findPendingEvents(
            @Param("maxRetries") int maxRetries);
}
