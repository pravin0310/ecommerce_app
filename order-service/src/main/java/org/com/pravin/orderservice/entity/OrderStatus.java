package org.com.pravin.orderservice.entity;

public enum OrderStatus {
    PENDING,
    PAYMENT_PROCESSING,
    PAYMENT_FAILED,
    INVENTORY_RESERVING,
    INVENTORY_FAILED,
    CONFIRMED,
    CANCELLED
}
