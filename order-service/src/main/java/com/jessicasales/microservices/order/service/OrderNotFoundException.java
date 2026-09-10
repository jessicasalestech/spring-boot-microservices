package com.jessicasales.microservices.order.service;

/**
 * Thrown when an order lookup by identifier returns no result.
 */
public class OrderNotFoundException extends RuntimeException {

    private final Long orderId;

    public OrderNotFoundException(Long orderId) {
        super("Order %d was not found".formatted(orderId));
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
