package org.com.pravin.orderservice.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.pravin.orderservice.kafka.event.OrderCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        // KEY = orderId → same order always goes to same partition → ordering guaranteed
        kafkaTemplate.send("order.created", event.getOrderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published order.created | orderId={} | partition={} | offset={}",
                                event.getOrderId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish order.created | orderId={} | error={}",
                                event.getOrderId(), ex.getMessage());
                    }
                });
    }

    public void publishOrderConfirmed(Long orderId,Long userId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("userId", userId);
        payload.put("status", "CONFIRMED");

        kafkaTemplate.send("order.confirmed", orderId.toString(), payload);
        log.info("Published order.confirmed | orderId={}", orderId);
    }

    public void publishOrderCancelled(Long orderId, Long userId, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("userId", userId);
        payload.put("reason", reason);
        payload.put("status", "CANCELLED");

        kafkaTemplate.send("order.cancelled", orderId.toString(), payload);
        log.info("Published order.cancelled | orderId={}", orderId);
    }
}
