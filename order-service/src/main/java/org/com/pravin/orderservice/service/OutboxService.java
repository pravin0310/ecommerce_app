package org.com.pravin.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.pravin.orderservice.entity.OutboxEvent;
import org.com.pravin.orderservice.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Save event to outbox table.
     * Called inside the SAME @Transactional as the business operation.
     * If business save fails → outbox save also rolls back. Atomic!
     */
    public void saveEvent(String aggregateId,
                          String aggregateType,
                          String eventType,
                          Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateId(aggregateId)
                    .aggregateType(aggregateType)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();

            outboxEventRepository.save(event);
            log.debug("Saved outbox event: type={}, aggregateId={}",
                    eventType, aggregateId);

        } catch (Exception e) {
            log.error("Failed to save outbox event: {}", e.getMessage());
            throw new RuntimeException("Failed to save outbox event", e);
        }
    }
}
