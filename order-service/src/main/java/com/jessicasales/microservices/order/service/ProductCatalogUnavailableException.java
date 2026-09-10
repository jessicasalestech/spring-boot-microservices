package com.jessicasales.microservices.order.service;

/**
 * Thrown when product-service cannot be reached or answers with a server error.
 */
public class ProductCatalogUnavailableException extends RuntimeException {

    private final Long productId;

    public ProductCatalogUnavailableException(Long productId, String message, Throwable cause) {
        super(message, cause);
        this.productId = productId;
    }

    public ProductCatalogUnavailableException(Long productId, Throwable cause) {
        this(productId, "The product catalogue is currently unavailable", cause);
    }

    public Long getProductId() {
        return productId;
    }
}
