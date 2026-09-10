package com.jessicasales.microservices.product.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence slice tests for {@link ProductRepository}.
 */
@DataJpaTest
@DisplayName("ProductRepository")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("saves a product and populates the auditing timestamps")
    void savesProductAndPopulatesTimestamps() {
        Product saved = productRepository.save(
                new Product("Mechanical Keyboard", "75% layout", new BigDecimal("429.90"), 25));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName("finds a product by its name ignoring case")
    void findsProductByNameIgnoringCase() {
        productRepository.save(new Product("Mechanical Keyboard", null, new BigDecimal("429.90"), 25));

        Optional<Product> found = productRepository.findByNameIgnoreCase("mechanical KEYBOARD");

        assertThat(found).isPresent();
        assertThat(found.get().getPrice()).isEqualByComparingTo("429.90");
    }

    @Test
    @DisplayName("reports whether a product name is already taken")
    void reportsExistingProductName() {
        productRepository.save(new Product("USB-C Docking Station", null, new BigDecimal("645.50"), 8));

        assertThat(productRepository.existsByNameIgnoreCase("usb-c docking station")).isTrue();
        assertThat(productRepository.existsByNameIgnoreCase("Unknown Product")).isFalse();
    }

    @Test
    @DisplayName("returns only active products")
    void returnsOnlyActiveProducts() {
        Product active = new Product("Active Product", null, new BigDecimal("10.00"), 5);
        Product inactive = new Product("Inactive Product", null, new BigDecimal("20.00"), 5);
        inactive.setActive(false);
        productRepository.saveAll(List.of(active, inactive));

        List<Product> result = productRepository.findByActiveTrue();

        assertThat(result).extracting(Product::getName).containsExactly("Active Product");
    }

    @Test
    @DisplayName("deletes a product by id")
    void deletesProductById() {
        Product saved = productRepository.save(
                new Product("Developer Laptop Stand", null, new BigDecimal("189.99"), 40));

        productRepository.deleteById(saved.getId());

        assertThat(productRepository.findById(saved.getId())).isEmpty();
    }
}
