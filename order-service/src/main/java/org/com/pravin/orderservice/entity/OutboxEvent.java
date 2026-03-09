package org.com.pravin.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateId;    // orderId

    @Column(nullable = false)
    private String aggregateType;  // "ORDER"

    @Column(nullable = false)
    private String eventType;      // "order.created"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;        // JSON string of the event

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    private String errorMessage;

    private Integer retryCount;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    public enum OutboxStatus {
        PENDING,     // waiting to be published
        PUBLISHED,   // successfully sent to Kafka
        FAILED       // failed after max retries
    }
}
