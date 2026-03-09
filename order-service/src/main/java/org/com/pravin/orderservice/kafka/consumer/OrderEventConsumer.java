package org.com.pravin.orderservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.pravin.orderservice.kafka.event.InventoryReservedEvent;
import org.com.pravin.orderservice.kafka.event.PaymentProcessedEvent;
import org.com.pravin.orderservice.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {
    private final OrderService orderService;

    @KafkaListener(
            topics = {"payment.processed", "payment.failed"},
            groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentEvent(
            @Payload PaymentProcessedEvent event, Acknowledgment ack) {

        log.info("Received payment event | orderId={} | status={}",
                event.getOrderId(), event.getStatus());
        try {
            if ("SUCCESS".equals(event.getStatus())) {
                orderService.markPaymentProcessing(event.getOrderId());
            } else {
                orderService.cancelOrder(event.getOrderId(),
                        "Payment failed: " + event.getFailureReason());
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error handling payment event: {}", e.getMessage());
            throw e;
        }
    }

    @KafkaListener(
            topics = {"inventory.reserved", "inventory.failed"},
            groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleInventoryEvent(
            @Payload InventoryReservedEvent event, Acknowledgment ack) {

        log.info("Received inventory event | orderId={} | status={}",
                event.getOrderId(), event.getStatus());
        try {
            if ("RESERVED".equals(event.getStatus())) {
                orderService.confirmOrder(event.getOrderId());
            } else {
                orderService.cancelOrder(event.getOrderId(),
                        "Inventory failed: " + event.getFailureReason());
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error handling inventory event: {}", e.getMessage());
            throw e;
        }
    }
}
