package com.jessicasales.microservices.order.service;

/**
 * Thrown when the requested product exists but cannot be ordered.
 */
public class ProductNotOrderableException extends RuntimeException {

    private final Long productId;

    public ProductNotOrderableException(Long productId, String reason) {
        super("Product %d cannot be ordered: %s".formatted(productId, reason));
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}
