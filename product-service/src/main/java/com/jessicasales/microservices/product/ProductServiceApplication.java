package com.jessicasales.microservices.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the product catalogue service.
 *
 * <p>Exposes a REST API over the {@code /api/products} resource and owns the
 * {@code products} table in its own H2 database instance.</p>
 */
@SpringBootApplication
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
