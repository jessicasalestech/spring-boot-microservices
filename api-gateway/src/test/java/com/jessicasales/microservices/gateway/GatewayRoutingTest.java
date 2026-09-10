package com.jessicasales.microservices.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end routing test: a real gateway instance is started and a lightweight
 * HTTP stub stands in for product-service. A request to the public gateway path
 * is asserted to be proxied to the stub.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Gateway routing")
class GatewayRoutingTest {

    private static HttpServer productStub;
    private static int productStubPort;
    private static int productRequestCount;

    @BeforeAll
    static void startProductStub() throws IOException {
        productStub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        productStub.createContext("/api/products", exchange -> {
            productRequestCount++;
            byte[] body = "{\"id\": 1, \"name\": \"From Product Stub\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        productStub.start();
        productStubPort = productStub.getAddress().getPort();
    }

    @AfterAll
    static void stopProductStub() {
        productStub.stop(0);
    }

    @DynamicPropertySource
    static void pointGatewayAtStub(DynamicPropertyRegistry registry) {
        // The application.yml route uses the PRODUCT_SERVICE_URL placeholder, so we
        // satisfy it through the environment rather than rewriting the routes list
        // (partially overriding a YAML list element would drop its predicates).
        registry.add("PRODUCT_SERVICE_URL", () -> "http://127.0.0.1:" + productStubPort);
    }

    @Test
    @DisplayName("proxies /api/products/{id} to product-service")
    void proxiesProductsToProductService(@Value("${local.server.port}") int port) {
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + port)
                .build();

        client.get().uri("/api/products/1")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("From Product Stub");

        assertThat(productRequestCount).as("the stub should have been called").isEqualTo(1);
    }

    @Test
    @DisplayName("passes unrecognised paths through to the 5xx/no-route handling")
    void rejectsUnknownPaths(@Value("${local.server.port}") int port) {
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + port)
                .build();

        // No route matches /api/unknown, so the gateway must not proxy anything.
        client.get().uri("/api/unknown").exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }
}