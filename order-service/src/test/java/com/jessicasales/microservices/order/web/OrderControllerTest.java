package com.jessicasales.microservices.order.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jessicasales.microservices.order.domain.Order;
import com.jessicasales.microservices.order.domain.OrderRepository;
import com.jessicasales.microservices.order.domain.OrderStatus;
import com.jessicasales.microservices.order.support.StubProductServiceConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer tests for {@link OrderController}.
 *
 * <p>The downstream {@code product-service} call is stubbed with
 * {@link MockRestServiceServer}, so the REST contract and the inter-service
 * contract are both verified without starting a second application.</p>
 */
@SpringBootTest(properties = {
        "product-service.stub-client=true",
        "product-service.base-url=http://product-service.test"
})
@AutoConfigureMockMvc
@Import(StubProductServiceConfiguration.class)
@DisplayName("OrderController")
class OrderControllerTest {

    private static final String PRODUCT_ENDPOINT = "http://product-service.test/api/products/";

    private static final String KEYBOARD_JSON = """
            {
              "id": 1,
              "name": "Mechanical Keyboard",
              "description": "Hot-swappable, 75% layout",
              "price": 429.90,
              "stockQuantity": 25,
              "active": true
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MockRestServiceServer productService;

    @BeforeEach
    void resetState() {
        orderRepository.deleteAll();
        productService.reset();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private void stubProduct(Long productId, String body) {
        productService.expect(requestTo(PRODUCT_ENDPOINT + productId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private Order persistOrder(Long productId, int quantity) {
        BigDecimal unitPrice = new BigDecimal("429.90");
        return orderRepository.save(new Order(productId, "Mechanical Keyboard", quantity,
                unitPrice, unitPrice.multiply(BigDecimal.valueOf(quantity)), OrderStatus.CONFIRMED));
    }

    @Nested
    @DisplayName("POST /api/orders")
    class CreateOrder {

        @Test
        @DisplayName("resolves the product downstream and stores the order with a computed total")
        void createsOrderFromCatalogueData() throws Exception {
            stubProduct(1L, KEYBOARD_JSON);

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("productId", 1, "quantity", 2))))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.productId").value(1))
                    .andExpect(jsonPath("$.productName").value("Mechanical Keyboard"))
                    .andExpect(jsonPath("$.quantity").value(2))
                    .andExpect(jsonPath("$.unitPrice").value(429.90))
                    .andExpect(jsonPath("$.totalPrice").value(859.80))
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));

            productService.verify();
            assertThat(orderRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("returns 404 when product-service reports an unknown product")
        void returnsNotFoundForUnknownProduct() throws Exception {
            productService.expect(requestTo(PRODUCT_ENDPOINT + 99))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND));

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("productId", 99, "quantity", 1))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Product not found"))
                    .andExpect(jsonPath("$.productId").value(99));

            productService.verify();
            assertThat(orderRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("returns 503 when product-service fails")
        void returnsServiceUnavailableOnServerError() throws Exception {
            productService.expect(requestTo(PRODUCT_ENDPOINT + 5))
                    .andRespond(withServerError());

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("productId", 5, "quantity", 1))))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.title").value("Product catalogue unavailable"));

            productService.verify();
            assertThat(orderRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("returns 503 when product-service is unreachable")
        void returnsServiceUnavailableWhenUnreachable() throws Exception {
            productService.expect(requestTo(PRODUCT_ENDPOINT + 6))
                    .andRespond(withException(new IOException("Connection refused")));

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("productId", 6, "quantity", 1))))
                    .andExpect(status().isServiceUnavailable());

            assertThat(orderRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("returns 409 when the requested quantity exceeds the stock")
        void returnsConflictWhenStockIsInsufficient() throws Exception {
            stubProduct(1L, """
                    {"id": 1, "name": "Mechanical Keyboard", "price": 429.90, "stockQuantity": 1, "active": true}
                    """);

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("productId", 1, "quantity", 5))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Insufficient stock"))
                    .andExpect(jsonPath("$.available").value(1))
                    .andExpect(jsonPath("$.requested").value(5));

            assertThat(orderRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("returns 409 when the product is no longer active")
        void returnsConflictWhenProductIsInactive() throws Exception {
            stubProduct(1L, """
                    {"id": 1, "name": "Retired Keyboard", "price": 429.90, "stockQuantity": 10, "active": false}
                    """);

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("productId", 1, "quantity", 1))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Product not orderable"));

            assertThat(orderRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("returns 400 without calling product-service when the payload is invalid")
        void returnsBadRequestForInvalidPayload() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("productId", 1, "quantity", 0))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation failed"))
                    .andExpect(jsonPath("$.violations.quantity").value("quantity must be greater than zero"));

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations.productId").exists())
                    .andExpect(jsonPath("$.violations.quantity").exists());

            assertThat(orderRepository.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/orders")
    class ReadOrders {

        @Test
        @DisplayName("lists the stored orders")
        void listsOrders() throws Exception {
            persistOrder(1L, 1);
            persistOrder(2L, 3);

            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("returns an empty array when there are no orders")
        void returnsEmptyList() throws Exception {
            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("returns a single order by id")
        void returnsOrderById() throws Exception {
            Order order = persistOrder(1L, 2);

            mockMvc.perform(get("/api/orders/{id}", order.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(order.getId()))
                    .andExpect(jsonPath("$.quantity").value(2))
                    .andExpect(jsonPath("$.totalPrice").value(859.80))
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("returns 404 when the order does not exist")
        void returnsNotFoundForUnknownOrder() throws Exception {
            mockMvc.perform(get("/api/orders/{id}", 123456L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Order not found"))
                    .andExpect(jsonPath("$.orderId").value(123456));
        }
    }
}
