package org.com.pravin.paymentservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
public class PaymentGatewayService {
    private final Random random = new Random();

    public String charge(Long userId, BigDecimal amount) {
        log.info("Calling payment gateway | userId={} | amount={}", userId, amount);

        // Simulate processing time
        try { Thread.sleep(100); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 80% success, 20% failure — for testing saga failure path
        if (random.nextInt(10) < 2) {
            throw new RuntimeException("Insufficient funds");
        }

        return "TXN-" + UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase();
    }
}
