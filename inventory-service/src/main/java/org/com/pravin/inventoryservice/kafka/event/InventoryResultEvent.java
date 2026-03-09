package org.com.pravin.inventoryservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResultEvent {
    private Long orderId;
    private String status;        // RESERVED or FAILED
    private String failureReason;
}
