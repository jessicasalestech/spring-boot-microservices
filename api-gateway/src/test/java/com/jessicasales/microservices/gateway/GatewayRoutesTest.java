package com.jessicasales.microservices.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.PropertiesRouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the routes declared in {@code application.yml} are bound into the
 * route definition repository with the expected ids, target URIs and predicates.
 */
@SpringBootTest
@DisplayName("Gateway route configuration")
class GatewayRoutesTest {

    @Autowired
    private PropertiesRouteDefinitionLocator routeDefinitionLocator;

    @Test
    @DisplayName("declares the expected number of routes")
    void declaresExpectedRoutes() {
        List<String> ids = routeDefinitions().stream()
                .map(RouteDefinition::getId)
                .toList();

        assertThat(ids).containsExactly("product-service", "order-service");
    }

    @Test
    @DisplayName("points the product routes at product-service")
    void pointsProductsAtProductService() {
        RouteDefinition route = route("product-service");

        assertThat(route.getUri().toString()).isEqualTo("http://localhost:8081");
        assertThat(route.getPredicates())
                .singleElement()
                .satisfies(predicate -> {
                    assertThat(predicate.getName()).isEqualTo("Path");
                    assertThat(predicate.getArgs()).containsEntry("_genkey_0", "/api/products/**");
                });
    }

    @Test
    @DisplayName("points the order routes at order-service")
    void pointsOrdersAtOrderService() {
        RouteDefinition route = route("order-service");

        assertThat(route.getUri().toString()).isEqualTo("http://localhost:8082");
        assertThat(route.getPredicates())
                .singleElement()
                .satisfies(predicate -> {
                    assertThat(predicate.getName()).isEqualTo("Path");
                    assertThat(predicate.getArgs()).containsEntry("_genkey_0", "/api/orders/**");
                });
    }

    private List<RouteDefinition> routeDefinitions() {
        return routeDefinitionLocator.getRouteDefinitions().collectList().block();
    }

    private RouteDefinition route(String id) {
        return routeDefinitions().stream()
                .filter(route -> route.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Route '%s' not found".formatted(id)));
    }
}