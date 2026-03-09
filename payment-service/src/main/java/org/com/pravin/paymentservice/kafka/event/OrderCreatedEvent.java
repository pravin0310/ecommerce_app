package org.com.pravin.paymentservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private Long userId;
    private BigDecimal totalAmount;
    private List<OrderItemDto> items;

    @Data
    @NoArgsConstructor @AllArgsConstructor
    public static class OrderItemDto {
        private Long productId;
        private Integer quantity;
        private BigDecimal price;
    }
}
