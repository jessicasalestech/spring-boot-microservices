package com.jessicasales.microservices.order.client;

import com.jessicasales.microservices.order.service.ProductCatalogUnavailableException;
import com.jessicasales.microservices.order.service.ProductNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for the {@code product-service} REST API.
 *
 * <p>Failures are translated into domain exceptions so the web layer never has to
 * reason about HTTP status codes coming from a downstream service:</p>
 * <ul>
 *   <li>{@code 404} to {@link ProductNotFoundException} (maps to HTTP 404)</li>
 *   <li>connection errors and {@code 5xx} to {@link ProductCatalogUnavailableException} (maps to HTTP 503)</li>
 * </ul>
 */
@Component
public class ProductCatalogClient {

    private static final Logger log = LoggerFactory.getLogger(ProductCatalogClient.class);

    private final RestClient restClient;

    public ProductCatalogClient(RestClient productServiceRestClient) {
        this.restClient = productServiceRestClient;
    }

    /**
     * Fetches a product from the catalogue.
     *
     * @param productId identifier of the product to fetch
     * @return the product read model
     * @throws ProductNotFoundException          when product-service answers 404
     * @throws ProductCatalogUnavailableException when product-service is unreachable or fails
     */
    public ProductDto fetchProduct(Long productId) {
        try {
            ProductDto product = restClient.get()
                    .uri("/api/products/{id}", productId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(),
                            (request, response) -> {
                                throw new ProductNotFoundException(productId);
                            })
                    .onStatus(HttpStatusCode::isError,
                            (request, response) -> {
                                throw new ProductCatalogUnavailableException(productId,
                                        "product-service answered HTTP %d".formatted(response.getStatusCode().value()),
                                        null);
                            })
                    .body(ProductDto.class);

            if (product == null || product.id() == null) {
                throw new ProductCatalogUnavailableException(productId,
                        "product-service returned an empty payload", null);
            }
            return product;
        } catch (ResourceAccessException exception) {
            log.warn("product-service is unreachable while fetching product {}", productId, exception);
            throw new ProductCatalogUnavailableException(productId, exception);
        }
    }
}
