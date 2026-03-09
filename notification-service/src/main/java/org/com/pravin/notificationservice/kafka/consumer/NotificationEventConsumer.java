package org.com.pravin.notificationservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.pravin.notificationservice.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {
    private final NotificationService notificationService;

    @KafkaListener(
            topics = "order.confirmed",
            groupId = "notification-service"
    )
    public void handleOrderConfirmed(
            @Payload Map<String, Object> payload,
            Acknowledgment ack) {

        log.info("Received order.confirmed event: {}", payload);

        try {
            Long orderId = Long.valueOf(payload.get("orderId").toString());
            Long userId = Long.valueOf(payload.get("userId").toString());
            notificationService.sendOrderConfirmed(orderId, userId);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process order.confirmed: {}", e.getMessage());
            throw e;
        }
    }

    @KafkaListener(
            topics = "order.cancelled",
            groupId = "notification-service"
    )
    public void handleOrderCancelled(
            @Payload Map<String, Object> payload,
            Acknowledgment ack) {

        log.info("Received order.cancelled event: {}", payload);

        try {
            Long orderId = Long.valueOf(payload.get("orderId").toString());
            Long userId = Long.valueOf(payload.get("userId") != null
                    ? payload.get("userId").toString() : "0");
            String reason = payload.getOrDefault("reason", "Unknown").toString();
            notificationService.sendOrderCancelled(orderId, userId, reason);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process order.cancelled: {}", e.getMessage());
            throw e;
        }
    }
}
