package com.jessicasales.microservices.order.service;

/**
 * Thrown when the requested quantity is larger than the available stock.
 */
public class InsufficientStockException extends RuntimeException {

    private final Long productId;
    private final int available;
    private final int requested;

    public InsufficientStockException(Long productId, int available, int requested) {
        super("Product %d has %d unit(s) available but %d were requested"
                .formatted(productId, available, requested));
        this.productId = productId;
        this.available = available;
        this.requested = requested;
    }

    public Long getProductId() {
        return productId;
    }

    public int getAvailable() {
        return available;
    }

    public int getRequested() {
        return requested;
    }
}
