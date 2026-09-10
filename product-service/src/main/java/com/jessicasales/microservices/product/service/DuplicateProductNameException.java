package com.jessicasales.microservices.product.service;

/**
 * Thrown when a product would violate the unique product name constraint.
 */
public class DuplicateProductNameException extends RuntimeException {

    private final String name;

    public DuplicateProductNameException(String name) {
        super("A product named '%s' already exists".formatted(name));
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
