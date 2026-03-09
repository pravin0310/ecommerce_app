package org.com.pravin.paymentservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultEvent {
    private Long orderId;
    private String status;
    private String transactionId;
    private String failureReason;
    private List<OrderCreatedEvent.OrderItemDto> items;
}
