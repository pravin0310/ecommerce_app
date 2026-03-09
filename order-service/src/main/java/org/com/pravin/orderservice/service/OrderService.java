package org.com.pravin.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.pravin.orderservice.client.ProductClient;
import org.com.pravin.orderservice.dto.OrderRequest;
import org.com.pravin.orderservice.dto.OrderResponse;
import org.com.pravin.orderservice.entity.Order;
import org.com.pravin.orderservice.entity.OrderItem;
import org.com.pravin.orderservice.entity.OrderStatus;
import org.com.pravin.orderservice.kafka.event.OrderCreatedEvent;
import org.com.pravin.orderservice.kafka.producer.OrderEventProducer;
import org.com.pravin.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
//    private final OrderRepository orderRepository;
//    private final OrderEventProducer orderEventProducer;
//    private final ProductClient productClient;
//
//    @Transactional
//    public OrderResponse placeOrder(OrderRequest request) {
//        log.info("Placing order for userId={}", request.getUserId());
//
//        // Build items
//        List<OrderItem> items = request.getItems().stream()
//                .map(i -> OrderItem.builder()
//                        .productId(i.getProductId())
//                        .quantity(i.getQuantity())
//                        .price(i.getPrice())
//                        .build())
//                .toList();
//
//        // Calculate total
//        BigDecimal total = items.stream()
//                .map(i -> i.getPrice()
//                        .multiply(BigDecimal.valueOf(i.getQuantity())))
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        // Save order as PENDING
//        Order order = Order.builder()
//                .userId(request.getUserId())
//                .status(OrderStatus.PENDING)
//                .totalAmount(total)
//                .items(items)
//                .build();
//
//        items.forEach(item -> item.setOrder(order));
//        orderRepository.save(order);
//        log.info("Order {} saved as PENDING", order.getId());
//
//        // Publish to Kafka → triggers Payment Service
//        OrderCreatedEvent event = buildEvent(order);
//        orderEventProducer.publishOrderCreated(event);
//
//        return OrderResponse.from(order);
//    }
//
//    @Transactional
//    public void markPaymentProcessing(Long orderId) {
//        updateStatus(orderId, OrderStatus.PAYMENT_PROCESSING, null);
//    }
//
//    @Transactional
//    public void confirmOrder(Long orderId) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new RuntimeException("Order not found"));
//        order.setStatus(OrderStatus.CONFIRMED);
//        orderRepository.save(order);
//        // Pass userId so notification service knows who to email
//        orderEventProducer.publishOrderConfirmed(orderId, order.getUserId());
//        log.info("Order {} CONFIRMED — saga complete!", orderId);
//    }
//
//    @Transactional
//    public void cancelOrder(Long orderId, String reason) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new RuntimeException("Order not found"));
//        order.setStatus(OrderStatus.CANCELLED);
//        order.setFailureReason(reason);
//        orderRepository.save(order);
//        orderEventProducer.publishOrderCancelled(orderId, order.getUserId(), reason);
//        log.warn("Order {} CANCELLED. Reason: {}", orderId, reason);
//    }
//
//    public OrderResponse getOrder(Long orderId) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
//        return OrderResponse.from(order);
//    }
//
//    public List<OrderResponse> getOrdersByUser(Long userId) {
//        return orderRepository.findByUserId(userId)
//                .stream().map(OrderResponse::from).toList();
//    }
//
//    private void updateStatus(Long orderId, OrderStatus status, String reason) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
//        order.setStatus(status);
//        order.setFailureReason(reason);
//        orderRepository.save(order);
//        log.info("Order {} → {}", orderId, status);
//    }
//
//    private OrderCreatedEvent buildEvent(Order order) {
//        List<OrderCreatedEvent.OrderItemDto> itemDtos = order.getItems().stream()
//                .map(i -> OrderCreatedEvent.OrderItemDto.builder()
//                        .productId(i.getProductId())
//                        .quantity(i.getQuantity())
//                        .price(i.getPrice())
//                        .build())
//                .toList();
//
//        return OrderCreatedEvent.builder()
//                .orderId(order.getId())
//                .userId(order.getUserId())
//                .totalAmount(order.getTotalAmount())
//                .items(itemDtos)
//                .build();
//    }

    private final OrderRepository orderRepository;
    private final OutboxService outboxService;       // ← replace OrderEventProducer
    private final ProductClient productClient;

    @Transactional  // ← covers BOTH order save AND outbox save
    public OrderResponse placeOrder(OrderRequest request) {
        log.info("Placing order for userId={}", request.getUserId());

        List<OrderItem> items = request.getItems().stream()
                .map(i -> OrderItem.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .price(i.getPrice())
                        .build())
                .toList();

        BigDecimal total = items.stream()
                .map(i -> i.getPrice()
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .userId(request.getUserId())
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .items(items)
                .build();

        items.forEach(item -> item.setOrder(order));
        orderRepository.save(order);

        // Build event payload
        OrderCreatedEvent event = buildEvent(order);

        // Save to outbox — SAME transaction as order save
        // If order save fails → outbox save rolls back too
        // If outbox save fails → order save rolls back too
        // Guaranteed: both saved or neither saved
        outboxService.saveEvent(
                order.getId().toString(),
                "ORDER",
                "order.created",
                event
        );

        log.info("Order {} saved, outbox event queued", order.getId());
        return OrderResponse.from(order);
    }

    @Transactional
    public void confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "Order not found: " + orderId));
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        // Outbox for confirmed event too
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("userId", order.getUserId());
        payload.put("status", "CONFIRMED");

        outboxService.saveEvent(
                orderId.toString(),
                "ORDER",
                "order.confirmed",
                payload
        );

        log.info("Order {} CONFIRMED — outbox event queued", orderId);
    }

    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "Order not found: " + orderId));
        order.setStatus(OrderStatus.CANCELLED);
        order.setFailureReason(reason);
        orderRepository.save(order);

        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("userId", order.getUserId());
        payload.put("reason", reason);

        outboxService.saveEvent(
                orderId.toString(),
                "ORDER",
                "order.cancelled",
                payload
        );

        log.warn("Order {} CANCELLED — outbox event queued", orderId);
    }

    @Transactional
    public void markPaymentProcessing(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "Order not found: " + orderId));
        order.setStatus(OrderStatus.PAYMENT_PROCESSING);
        orderRepository.save(order);
    }

    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "Order not found: " + orderId));
        return OrderResponse.from(order);
    }

    public List<OrderResponse> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream().map(OrderResponse::from).toList();
    }

    private OrderCreatedEvent buildEvent(Order order) {
        List<OrderCreatedEvent.OrderItemDto> itemDtos = order.getItems()
                .stream()
                .map(i -> OrderCreatedEvent.OrderItemDto.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .price(i.getPrice())
                        .build())
                .toList();

        return OrderCreatedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .items(itemDtos)
                .build();
    }
}
