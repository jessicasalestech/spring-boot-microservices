package com.jessicasales.microservices.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Order} aggregates.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByProductId(Long productId);

    List<Order> findByStatus(OrderStatus status);
}
