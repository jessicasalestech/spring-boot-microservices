package com.jessicasales.microservices.order.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Wires the HTTP client used to call {@code product-service}.
 *
 * <p>The base URL, connect timeout and read timeout are all configurable, so the
 * service can be pointed at any environment without a rebuild.</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ProductServiceProperties.class)
public class ProductServiceClientConfiguration {

    /**
     * Production {@link RestClient}. Disabled when {@code product-service.stub-client=true}
     * so that a test slice can bind {@code MockRestServiceServer} to the builder instead.
     */
    @Bean
    @ConditionalOnProperty(prefix = "product-service", name = "stub-client",
            havingValue = "false", matchIfMissing = true)
    RestClient productServiceRestClient(RestClient.Builder builder,
                                        ProductServiceProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(properties.getReadTimeout());

        return builder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
