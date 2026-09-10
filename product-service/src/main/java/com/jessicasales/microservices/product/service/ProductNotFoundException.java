package com.jessicasales.microservices.product.service;

/**
 * Thrown when a product lookup by identifier returns no result.
 */
public class ProductNotFoundException extends RuntimeException {

    private final Long productId;

    public ProductNotFoundException(Long productId) {
        super("Product %d was not found".formatted(productId));
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}
