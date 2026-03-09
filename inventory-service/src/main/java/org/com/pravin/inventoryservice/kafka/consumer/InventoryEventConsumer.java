package org.com.pravin.inventoryservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.pravin.inventoryservice.kafka.event.InventoryResultEvent;
import org.com.pravin.inventoryservice.kafka.event.PaymentProcessedEvent;
import org.com.pravin.inventoryservice.service.InventoryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {
    private final InventoryService inventoryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
            topics = "payment.processed",
            groupId = "inventory-service"
    )
    public void handlePaymentProcessed(
            @Payload PaymentProcessedEvent event,
            Acknowledgment ack) {

        log.info("Received payment.processed | orderId={}",
                event.getOrderId());

        // Idempotency check
        if (inventoryService.isAlreadyReserved(event.getOrderId())) {
            log.warn("Stock already reserved for orderId={}. Skipping.",
                    event.getOrderId());
            ack.acknowledge();
            return;
        }

        try {
            inventoryService.reserveStock(
                    event.getOrderId(), event.getItems());

            // Publish success → Order Service confirms order
            kafkaTemplate.send(
                    "inventory.reserved",
                    event.getOrderId().toString(),
                    InventoryResultEvent.builder()
                            .orderId(event.getOrderId())
                            .status("RESERVED")
                            .build()
            );

            log.info("Inventory RESERVED | orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("Inventory FAILED | orderId={} | reason={}",
                    event.getOrderId(), e.getMessage());

            // Publish failure → Order Service cancels order
            kafkaTemplate.send(
                    "inventory.failed",
                    event.getOrderId().toString(),
                    InventoryResultEvent.builder()
                            .orderId(event.getOrderId())
                            .status("FAILED")
                            .failureReason(e.getMessage())
                            .build()
            );
        }

        ack.acknowledge();
    }
}
