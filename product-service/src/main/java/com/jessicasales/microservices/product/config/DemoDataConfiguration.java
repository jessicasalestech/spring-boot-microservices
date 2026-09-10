package com.jessicasales.microservices.product.config;

import com.jessicasales.microservices.product.domain.Product;
import com.jessicasales.microservices.product.domain.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds a handful of products when the {@code demo} profile is active.
 *
 * <p>Keeps the H2 in-memory database useful after {@code docker compose up}
 * without shipping SQL fixtures that would also run during tests.</p>
 */
@Configuration
@Profile("demo")
public class DemoDataConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DemoDataConfiguration.class);

    @Bean
    ApplicationRunner seedProducts(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() > 0) {
                return;
            }
            List<Product> products = List.of(
                    new Product("Mechanical Keyboard", "Hot-swappable, 75% layout", new BigDecimal("429.90"), 25),
                    new Product("Noise-Cancelling Headphones", "Over-ear, 30h battery", new BigDecimal("899.00"), 12),
                    new Product("USB-C Docking Station", "Dual 4K output, 90W passthrough", new BigDecimal("645.50"), 8),
                    new Product("Developer Laptop Stand", "Aluminium, adjustable height", new BigDecimal("189.99"), 40)
            );
            productRepository.saveAll(products);
            log.info("Seeded {} demo products", products.size());
        };
    }
}
