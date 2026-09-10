package com.jessicasales.microservices.order.service;

import com.jessicasales.microservices.order.client.ProductCatalogClient;
import com.jessicasales.microservices.order.client.ProductDto;
import com.jessicasales.microservices.order.domain.Order;
import com.jessicasales.microservices.order.domain.OrderRepository;
import com.jessicasales.microservices.order.domain.OrderStatus;
import com.jessicasales.microservices.order.web.dto.CreateOrderRequest;
import com.jessicasales.microservices.order.web.dto.OrderResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Application service holding the order use cases.
 *
 * <p>Creating an order is a small distributed workflow: the product is resolved
 * through {@code product-service} first, then the order is persisted with the
 * product snapshot and a computed total.</p>
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductCatalogClient productCatalogClient;

    public OrderService(OrderRepository orderRepository, ProductCatalogClient productCatalogClient) {
        this.orderRepository = orderRepository;
        this.productCatalogClient = productCatalogClient;
    }

    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .toList();
    }

    public OrderResponse findById(Long id) {
        return OrderResponse.from(requireOrder(id));
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        ProductDto product = productCatalogClient.fetchProduct(request.productId());

        if (!product.active()) {
            throw new ProductNotOrderableException(product.id(), "the product is no longer available");
        }

        int available = product.stockQuantity() == null ? 0 : product.stockQuantity();
        if (available < request.quantity()) {
            throw new InsufficientStockException(product.id(), available, request.quantity());
        }

        BigDecimal totalPrice = product.price()
                .multiply(BigDecimal.valueOf(request.quantity()));

        Order order = new Order(
                product.id(),
                product.name(),
                request.quantity(),
                product.price(),
                totalPrice,
                OrderStatus.CONFIRMED
        );

        return OrderResponse.from(orderRepository.save(order));
    }

    private Order requireOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
