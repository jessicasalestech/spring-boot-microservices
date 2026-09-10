package com.jessicasales.microservices.product.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jessicasales.microservices.product.domain.Product;
import com.jessicasales.microservices.product.domain.ProductRepository;
import com.jessicasales.microservices.product.web.dto.ProductRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end web layer tests for {@link ProductController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ProductController")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanDatabase() {
        productRepository.deleteAll();
    }

    private Product persist(String name, String description, String price, int stock) {
        return productRepository.save(new Product(name, description, new BigDecimal(price), stock));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Nested
    @DisplayName("POST /api/products")
    class CreateProduct {

        @Test
        @DisplayName("creates a product and returns 201 with a Location header")
        void createsProduct() throws Exception {
            ProductRequest request = new ProductRequest(
                    "Mechanical Keyboard", "Hot-swappable", new BigDecimal("429.90"), 25);

            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.name").value("Mechanical Keyboard"))
                    .andExpect(jsonPath("$.price").value(429.90))
                    .andExpect(jsonPath("$.stockQuantity").value(25))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("returns 400 when the name is blank")
        void rejectsBlankName() throws Exception {
            String payload = json(Map.of("name", "   ", "price", 10.00, "stockQuantity", 1));

            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation failed"))
                    .andExpect(jsonPath("$.violations.name").value("name must not be blank"));
        }

        @Test
        @DisplayName("returns 400 when the price is not positive")
        void rejectsNonPositivePrice() throws Exception {
            String payload = json(Map.of("name", "Cheap Product", "price", -5.00, "stockQuantity", 1));

            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations.price").value("price must be greater than zero"));
        }

        @Test
        @DisplayName("returns 400 when the stock quantity is negative")
        void rejectsNegativeStock() throws Exception {
            String payload = json(Map.of("name", "Odd Product", "price", 5.00, "stockQuantity", -1));

            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations.stockQuantity").value("stockQuantity must not be negative"));
        }

        @Test
        @DisplayName("returns 400 when required fields are missing")
        void rejectsMissingFields() throws Exception {
            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations.name").exists())
                    .andExpect(jsonPath("$.violations.price").exists())
                    .andExpect(jsonPath("$.violations.stockQuantity").exists());
        }

        @Test
        @DisplayName("returns 409 when the product name is already taken")
        void rejectsDuplicateName() throws Exception {
            persist("Mechanical Keyboard", null, "429.90", 25);
            ProductRequest request = new ProductRequest(
                    "mechanical keyboard", null, new BigDecimal("100.00"), 3);

            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Duplicate product name"))
                    .andExpect(jsonPath("$.status").value(409));
        }
    }

    @Nested
    @DisplayName("GET /api/products")
    class ListProducts {

        @Test
        @DisplayName("returns the whole catalogue")
        void returnsAllProducts() throws Exception {
            persist("Mechanical Keyboard", null, "429.90", 25);
            persist("USB-C Docking Station", null, "645.50", 8);

            mockMvc.perform(get("/api/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].name", containsInAnyOrder(
                            "Mechanical Keyboard", "USB-C Docking Station")));
        }

        @Test
        @DisplayName("returns an empty array when the catalogue is empty")
        void returnsEmptyCatalogue() throws Exception {
            mockMvc.perform(get("/api/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/products/{id}")
    class GetProduct {

        @Test
        @DisplayName("returns the requested product")
        void returnsProduct() throws Exception {
            Product product = persist("Noise-Cancelling Headphones", "Over-ear", "899.00", 12);

            mockMvc.perform(get("/api/products/{id}", product.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(product.getId()))
                    .andExpect(jsonPath("$.name").value("Noise-Cancelling Headphones"))
                    .andExpect(jsonPath("$.description").value("Over-ear"));
        }

        @Test
        @DisplayName("returns 404 with a problem detail when the product does not exist")
        void returnsNotFound() throws Exception {
            mockMvc.perform(get("/api/products/{id}", 999_999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Product not found"))
                    .andExpect(jsonPath("$.productId").value(999_999))
                    .andExpect(jsonPath("$.detail", containsString("999999")));
        }
    }

    @Nested
    @DisplayName("PUT /api/products/{id}")
    class UpdateProduct {

        @Test
        @DisplayName("updates an existing product")
        void updatesProduct() throws Exception {
            Product product = persist("Old Name", "Old description", "10.00", 1);
            ProductRequest request = new ProductRequest(
                    "New Name", "New description", new BigDecimal("20.00"), 5);

            mockMvc.perform(put("/api/products/{id}", product.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("New Name"))
                    .andExpect(jsonPath("$.description").value("New description"))
                    .andExpect(jsonPath("$.price").value(20.00))
                    .andExpect(jsonPath("$.stockQuantity").value(5));
        }

        @Test
        @DisplayName("returns 404 when the product does not exist")
        void returnsNotFound() throws Exception {
            ProductRequest request = new ProductRequest(
                    "Any Product", null, new BigDecimal("20.00"), 5);

            mockMvc.perform(put("/api/products/{id}", 4242L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Product not found"));
        }

        @Test
        @DisplayName("returns 409 when renaming onto another product's name")
        void rejectsDuplicateName() throws Exception {
            persist("Existing Product", null, "10.00", 1);
            Product target = persist("Other Product", null, "10.00", 1);
            ProductRequest request = new ProductRequest(
                    "Existing Product", null, new BigDecimal("10.00"), 1);

            mockMvc.perform(put("/api/products/{id}", target.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 400 when the payload is invalid")
        void rejectsInvalidPayload() throws Exception {
            Product product = persist("Existing Product", null, "10.00", 1);

            mockMvc.perform(put("/api/products/{id}", product.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("name", "", "price", 0, "stockQuantity", -3))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation failed"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/products/{id}")
    class DeleteProduct {

        @Test
        @DisplayName("deletes the product and returns 204")
        void deletesProduct() throws Exception {
            Product product = persist("Developer Laptop Stand", null, "189.99", 40);

            mockMvc.perform(delete("/api/products/{id}", product.getId()))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/products/{id}", product.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 404 when the product does not exist")
        void returnsNotFound() throws Exception {
            mockMvc.perform(delete("/api/products/{id}", 123_456L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Product not found"));
        }
    }
}
