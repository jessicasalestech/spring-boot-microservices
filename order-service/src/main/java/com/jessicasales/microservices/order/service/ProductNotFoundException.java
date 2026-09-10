package com.jessicasales.microservices.order.service;

/**
 * Thrown when product-service reports that the referenced product does not exist.
 */
public class ProductNotFoundException extends RuntimeException {

    private final Long productId;

    public ProductNotFoundException(Long productId) {
        super("Product %d does not exist".formatted(productId));
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}
