package com.jessicasales.microservices.order.support;

import com.jessicasales.microservices.order.config.ProductServiceProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Test slice that replaces the real {@code product-service} {@link RestClient}
 * with one backed by {@link MockRestServiceServer}.
 *
 * <p>The builder is a singleton so that {@code MockRestServiceServer.bindTo(builder)}
 * installs the stub request factory <em>before</em> the client is built, and the
 * client bean declares the server as a dependency to guarantee that ordering.
 * Activate it together with {@code product-service.stub-client=true}, which disables
 * the production client bean.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class StubProductServiceConfiguration {

    @Bean
    RestClient.Builder productServiceRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    MockRestServiceServer mockRestServiceServer(RestClient.Builder productServiceRestClientBuilder) {
        return MockRestServiceServer.bindTo(productServiceRestClientBuilder).build();
    }

    @Bean
    RestClient productServiceRestClient(RestClient.Builder productServiceRestClientBuilder,
                                        MockRestServiceServer mockRestServiceServer,
                                        ProductServiceProperties properties) {
        return productServiceRestClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}
