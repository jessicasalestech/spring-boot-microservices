package com.jessicasales.microservices.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Read model of a product as returned by {@code GET /api/products/{id}}.
 *
 * <p>Deliberately decoupled from product-service's internal DTO: unknown fields
 * are ignored so the producer can evolve without breaking this consumer.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductDto(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        boolean active
) {
}
