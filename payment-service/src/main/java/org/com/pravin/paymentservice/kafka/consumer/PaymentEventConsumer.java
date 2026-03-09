package org.com.pravin.paymentservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.pravin.paymentservice.entity.Payment;
import org.com.pravin.paymentservice.kafka.event.OrderCreatedEvent;
import org.com.pravin.paymentservice.kafka.event.PaymentResultEvent;
import org.com.pravin.paymentservice.repository.PaymentRepository;
import org.com.pravin.paymentservice.service.PaymentGatewayService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(
            topics = "order.created",
            groupId = "payment-service"
    )
    @Transactional
    public void handleOrderCreated(
            @Payload OrderCreatedEvent event,
            Acknowledgment ack) {

        log.info("Received order.created | orderId={} | amount={}",
                event.getOrderId(), event.getTotalAmount());

        // IDEMPOTENCY CHECK — critical!
        // If Kafka replays this message, don't charge the customer twice
        if (paymentRepository.existsByOrderId(event.getOrderId())) {
            log.warn("Payment already processed for orderId={}. Skipping.",
                    event.getOrderId());
            ack.acknowledge();
            return;
        }

        // Save as PENDING first
        Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .amount(event.getTotalAmount())
                .status(Payment.PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        try {
            // Call mock payment gateway
            String transactionId = paymentGatewayService.charge(
                    event.getUserId(),
                    event.getTotalAmount()
            );

            // Update to SUCCESS
            payment.setStatus(Payment.PaymentStatus.SUCCESS);
            payment.setTransactionId(transactionId);
            paymentRepository.save(payment);

            // Publish success → Inventory Service picks this up next
            kafkaTemplate.send(
                    "payment.processed",
                    event.getOrderId().toString(),
                    PaymentResultEvent.builder()
                            .orderId(event.getOrderId())
                            .status("SUCCESS")
                            .transactionId(transactionId)
                            .items(event.getItems())
                            .build()
            );

            log.info("Payment SUCCESS | orderId={} | txId={}",
                    event.getOrderId(), transactionId);

        } catch (Exception e) {
            log.error("Payment FAILED | orderId={} | reason={}",
                    event.getOrderId(), e.getMessage());

            // Update to FAILED
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment);

            // Publish failure → Order Service cancels the order
            kafkaTemplate.send(
                    "payment.failed",
                    event.getOrderId().toString(),
                    PaymentResultEvent.builder()
                            .orderId(event.getOrderId())
                            .status("FAILED")
                            .failureReason(e.getMessage())
                            .build()
            );
        }

        ack.acknowledge();
    }
}
