package com.jessicasales.microservices.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies that the gateway Spring context starts (annotation wiring, property
 * binding, route definition repository).
 */
@SpringBootTest
@DisplayName("GatewayApplication context")
class GatewayApplicationTests {

    @Test
    @DisplayName("loads the application context")
    void contextLoads() {
        // context boot is the assertion
    }
}