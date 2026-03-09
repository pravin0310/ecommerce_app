package org.com.pravin.orderservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.pravin.orderservice.entity.OutboxEvent;
import org.com.pravin.orderservice.repository.OutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRIES = 3;

    /**
     * Runs every 5 seconds — picks up PENDING events and publishes to Kafka.
     * If Kafka is temporarily down, events stay PENDING and retry next cycle.
     * @Transactional ensures status update is saved even if other steps fail.
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents =
                outboxEventRepository.findPendingEvents(MAX_RETRIES);

        if (pendingEvents.isEmpty()) return;

        log.debug("Processing {} outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // Parse payload back to object
                Object payload = objectMapper.readValue(
                        event.getPayload(), Object.class);

                // Publish to Kafka — key = aggregateId (orderId)
                kafkaTemplate.send(
                        event.getEventType(),
                        event.getAggregateId(),
                        payload
                ).get(); // .get() makes it synchronous — wait for ack

                // Mark as published
                event.setStatus(OutboxEvent.OutboxStatus.PUBLISHED);
                event.setPublishedAt(LocalDateTime.now());
                outboxEventRepository.save(event);

                log.info("Published outbox event: type={}, aggregateId={}",
                        event.getEventType(), event.getAggregateId());

            } catch (Exception e) {
                log.error("Failed to publish outbox event id={}: {}",
                        event.getId(), e.getMessage());

                // Increment retry count
                event.setRetryCount(event.getRetryCount() + 1);
                event.setErrorMessage(e.getMessage());

                // Mark as FAILED after max retries
                if (event.getRetryCount() >= MAX_RETRIES) {
                    event.setStatus(OutboxEvent.OutboxStatus.FAILED);
                    log.error("Outbox event id={} FAILED after {} retries",
                            event.getId(), MAX_RETRIES);
                }

                outboxEventRepository.save(event);
            }
        }
    }
}
