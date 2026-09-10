package com.jessicasales.microservices.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration of the {@code product-service} downstream dependency.
 *
 * <p>Bound from the {@code product-service.*} keys in {@code application.yml}.</p>
 */
@ConfigurationProperties(prefix = "product-service")
public class ProductServiceProperties {

    /** Base URL of product-service, e.g. {@code http://localhost:8081}. */
    private String baseUrl = "http://localhost:8081";

    /** TCP connect timeout for the downstream calls. */
    private Duration connectTimeout = Duration.ofSeconds(2);

    /** Read timeout for the downstream calls. */
    private Duration readTimeout = Duration.ofSeconds(5);

    /**
     * Test seam: when {@code true} the production {@code RestClient} bean is not
     * created, so the test slice can bind {@code MockRestServiceServer} instead.
     */
    private boolean stubClient = false;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public boolean isStubClient() {
        return stubClient;
    }

    public void setStubClient(boolean stubClient) {
        this.stubClient = stubClient;
    }
}
