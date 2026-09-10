package com.jessicasales.microservices.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the order service.
 *
 * <p>Owns the {@code orders} table and talks to {@code product-service} over HTTP
 * to validate that the referenced product exists before an order is stored.</p>
 */
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
