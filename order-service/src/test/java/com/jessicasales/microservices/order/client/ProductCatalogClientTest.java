package com.jessicasales.microservices.order.client;

import com.jessicasales.microservices.order.service.ProductCatalogUnavailableException;
import com.jessicasales.microservices.order.service.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;

/**
 * Contract tests for the product-service HTTP client, using
 * {@link MockRestServiceServer} without starting a Spring context.
 */
@DisplayName("ProductCatalogClient")
class ProductCatalogClientTest {

    private static final String BASE_URL = "http://product-service.test";

    private MockRestServiceServer server;
    private ProductCatalogClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ProductCatalogClient(builder.baseUrl(BASE_URL).build());
    }

    @Test
    @DisplayName("maps a 200 response to a product read model")
    void mapsSuccessfulResponse() {
        server.expect(requestTo(BASE_URL + "/api/products/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": 1,
                          "name": "Mechanical Keyboard",
                          "description": "Hot-swappable",
                          "price": 429.90,
                          "stockQuantity": 25,
                          "active": true,
                          "unknownField": "ignored"
                        }
                        """, MediaType.APPLICATION_JSON));

        ProductDto product = client.fetchProduct(1L);

        assertThat(product.id()).isEqualTo(1L);
        assertThat(product.name()).isEqualTo("Mechanical Keyboard");
        assertThat(product.price()).isEqualByComparingTo(new BigDecimal("429.90"));
        assertThat(product.stockQuantity()).isEqualTo(25);
        assertThat(product.active()).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("translates a 404 into ProductNotFoundException")
    void translatesNotFound() {
        server.expect(requestTo(BASE_URL + "/api/products/99"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.fetchProduct(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
        server.verify();
    }

    @Test
    @DisplayName("translates a 500 into ProductCatalogUnavailableException")
    void translatesServerError() {
        server.expect(requestTo(BASE_URL + "/api/products/2"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.fetchProduct(2L))
                .isInstanceOf(ProductCatalogUnavailableException.class)
                .hasMessageContaining("500");
        server.verify();
    }

    @Test
    @DisplayName("translates a connection failure into ProductCatalogUnavailableException")
    void translatesConnectionFailure() {
        server.expect(requestTo(BASE_URL + "/api/products/3"))
                .andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> client.fetchProduct(3L))
                .isInstanceOf(ProductCatalogUnavailableException.class);
        server.verify();
    }

    @Test
    @DisplayName("translates an empty payload into ProductCatalogUnavailableException")
    void translatesEmptyPayload() {
        server.expect(requestTo(BASE_URL + "/api/products/4"))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(""));

        assertThatThrownBy(() -> client.fetchProduct(4L))
                .isInstanceOf(ProductCatalogUnavailableException.class);
        server.verify();
    }
}
