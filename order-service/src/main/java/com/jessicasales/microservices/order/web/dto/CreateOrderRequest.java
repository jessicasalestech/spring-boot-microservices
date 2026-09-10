package com.jessicasales.microservices.order.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Inbound payload for creating an order.
 */
public record CreateOrderRequest(

        @NotNull(message = "productId is required")
        @Positive(message = "productId must be a positive number")
        Long productId,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than zero")
        @Max(value = 1000, message = "quantity must be at most 1000")
        Integer quantity
) {
}
