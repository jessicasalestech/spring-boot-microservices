package com.jessicasales.microservices.order.domain;

/**
 * Lifecycle of an order.
 */
public enum OrderStatus {
    /** The order was accepted and the referenced product was validated. */
    CONFIRMED,
    /** The order was cancelled by the customer or an operator. */
    CANCELLED
}
