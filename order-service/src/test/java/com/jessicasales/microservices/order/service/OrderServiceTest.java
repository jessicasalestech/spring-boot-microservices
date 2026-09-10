package com.jessicasales.microservices.order.service;

import com.jessicasales.microservices.order.client.ProductCatalogClient;
import com.jessicasales.microservices.order.client.ProductDto;
import com.jessicasales.microservices.order.domain.Order;
import com.jessicasales.microservices.order.domain.OrderRepository;
import com.jessicasales.microservices.order.domain.OrderStatus;
import com.jessicasales.microservices.order.web.dto.CreateOrderRequest;
import com.jessicasales.microservices.order.web.dto.OrderResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Business rule tests for {@link OrderService} with a mocked catalogue client.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductCatalogClient productCatalogClient;

    @InjectMocks
    private OrderService orderService;

    private ProductDto product(boolean active, int stock, String price) {
        return new ProductDto(1L, "Mechanical Keyboard", "Hot-swappable",
                new BigDecimal(price), stock, active);
    }

    @Test
    @DisplayName("computes the total price from the catalogue unit price")
    void computesTotalPrice() {
        given(productCatalogClient.fetchProduct(1L)).willReturn(product(true, 25, "429.90"));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.create(new CreateOrderRequest(1L, 2));

        assertThat(response.productId()).isEqualTo(1L);
        assertThat(response.productName()).isEqualTo("Mechanical Keyboard");
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.unitPrice()).isEqualByComparingTo("429.90");
        assertThat(response.totalPrice()).isEqualByComparingTo("859.80");
        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("rejects an inactive product without touching the database")
    void rejectsInactiveProduct() {
        given(productCatalogClient.fetchProduct(1L)).willReturn(product(false, 25, "429.90"));

        assertThatThrownBy(() -> orderService.create(new CreateOrderRequest(1L, 1)))
                .isInstanceOf(ProductNotOrderableException.class)
                .hasMessageContaining("no longer available");

        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("rejects a quantity above the available stock")
    void rejectsQuantityAboveStock() {
        given(productCatalogClient.fetchProduct(1L)).willReturn(product(true, 2, "429.90"));

        assertThatThrownBy(() -> orderService.create(new CreateOrderRequest(1L, 5)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("2 unit(s) available");

        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("propagates a missing product from the catalogue")
    void propagatesMissingProduct() {
        given(productCatalogClient.fetchProduct(7L)).willThrow(new ProductNotFoundException(7L));

        assertThatThrownBy(() -> orderService.create(new CreateOrderRequest(7L, 1)))
                .isInstanceOf(ProductNotFoundException.class);

        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("returns the stored order when looking it up by id")
    void findsOrderById() {
        Order order = new Order(1L, "Mechanical Keyboard", 2, new BigDecimal("429.90"),
                new BigDecimal("859.80"), OrderStatus.CONFIRMED);
        given(orderRepository.findById(1L)).willReturn(java.util.Optional.of(order));

        OrderResponse response = orderService.findById(1L);

        assertThat(response.totalPrice()).isEqualByComparingTo("859.80");
    }

    @Test
    @DisplayName("throws when the order does not exist")
    void throwsWhenOrderMissing() {
        given(orderRepository.findById(404L)).willReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> orderService.findById(404L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("404");
    }
}
