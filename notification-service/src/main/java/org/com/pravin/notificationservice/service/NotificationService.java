package org.com.pravin.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {
    // In production: inject JavaMailSender or Twilio SDK here

    public void sendOrderConfirmed(Long orderId, Long userId) {
        log.info("====================================");
        log.info("EMAIL SENT to userId={}:", userId);
        log.info("Subject: Your order #{} is CONFIRMED!", orderId);
        log.info("Body: Your payment was successful and");
        log.info("      your items are reserved. Thank you!");
        log.info("====================================");
    }

    public void sendOrderCancelled(Long orderId, Long userId, String reason) {
        log.info("====================================");
        log.info("EMAIL SENT to userId={}:", userId);
        log.info("Subject: Your order #{} was CANCELLED", orderId);
        log.info("Body: Unfortunately your order was cancelled.");
        log.info("      Reason: {}", reason);
        log.info("      If payment was charged, refund in 3-5 days.");
        log.info("====================================");
    }
}
