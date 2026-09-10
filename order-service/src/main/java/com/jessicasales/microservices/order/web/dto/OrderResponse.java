package com.jessicasales.microservices.order.web.dto;

import com.jessicasales.microservices.order.domain.Order;
import com.jessicasales.microservices.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Outbound representation of an order.
 */
public record OrderResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        OrderStatus status,
        Instant createdAt
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getProductId(),
                order.getProductName(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
