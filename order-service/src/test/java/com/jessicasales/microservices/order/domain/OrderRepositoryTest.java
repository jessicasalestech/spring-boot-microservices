package com.jessicasales.microservices.order.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence slice tests for {@link OrderRepository}.
 */
@DataJpaTest
@DisplayName("OrderRepository")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    private Order newOrder(Long productId, int quantity, OrderStatus status) {
        BigDecimal unitPrice = new BigDecimal("429.90");
        return new Order(productId, "Mechanical Keyboard", quantity, unitPrice,
                unitPrice.multiply(BigDecimal.valueOf(quantity)), status);
    }

    @Test
    @DisplayName("persists an order and populates the creation timestamp")
    void persistsOrder() {
        Order saved = orderRepository.save(newOrder(1L, 2, OrderStatus.CONFIRMED));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("859.80");
    }

    @Test
    @DisplayName("finds orders by product id")
    void findsByProductId() {
        orderRepository.saveAll(List.of(
                newOrder(1L, 1, OrderStatus.CONFIRMED),
                newOrder(1L, 3, OrderStatus.CONFIRMED),
                newOrder(2L, 1, OrderStatus.CANCELLED)));

        List<Order> result = orderRepository.findByProductId(1L);

        assertThat(result).hasSize(2)
                .allSatisfy(order -> assertThat(order.getProductId()).isEqualTo(1L));
    }

    @Test
    @DisplayName("finds orders by status")
    void findsByStatus() {
        orderRepository.saveAll(List.of(
                newOrder(1L, 1, OrderStatus.CONFIRMED),
                newOrder(2L, 1, OrderStatus.CANCELLED)));

        assertThat(orderRepository.findByStatus(OrderStatus.CANCELLED)).hasSize(1);
        assertThat(orderRepository.findByStatus(OrderStatus.CONFIRMED)).hasSize(1);
    }

    @Test
    @DisplayName("returns an empty result when the order does not exist")
    void returnsEmptyWhenMissing() {
        Optional<Order> result = orderRepository.findById(404L);

        assertThat(result).isEmpty();
    }
}
