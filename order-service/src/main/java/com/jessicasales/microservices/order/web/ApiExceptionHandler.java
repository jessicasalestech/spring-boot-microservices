package com.jessicasales.microservices.order.web;

import com.jessicasales.microservices.order.service.InsufficientStockException;
import com.jessicasales.microservices.order.service.OrderNotFoundException;
import com.jessicasales.microservices.order.service.ProductCatalogUnavailableException;
import com.jessicasales.microservices.order.service.ProductNotOrderableException;
import com.jessicasales.microservices.order.service.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates application exceptions into RFC 7807 {@code ProblemDetail} responses.
 *
 * <p>The distinction between the downstream failures matters for clients: a missing
 * product is a {@code 404}, a business rule violation is a {@code 409} and an
 * unreachable dependency is a {@code 503}.</p>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final URI TYPE_BASE = URI.create("https://api.jessicasales.dev/problems/");

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFound(OrderNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Order not found");
        problem.setType(TYPE_BASE.resolve("order-not-found"));
        problem.setProperty("orderId", exception.getOrderId());
        return problem;
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Product not found");
        problem.setType(TYPE_BASE.resolve("product-not-found"));
        problem.setProperty("productId", exception.getProductId());
        return problem;
    }

    @ExceptionHandler(ProductNotOrderableException.class)
    public ProblemDetail handleProductNotOrderable(ProductNotOrderableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Product not orderable");
        problem.setType(TYPE_BASE.resolve("product-not-orderable"));
        problem.setProperty("productId", exception.getProductId());
        return problem;
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Insufficient stock");
        problem.setType(TYPE_BASE.resolve("insufficient-stock"));
        problem.setProperty("productId", exception.getProductId());
        problem.setProperty("available", exception.getAvailable());
        problem.setProperty("requested", exception.getRequested());
        return problem;
    }

    @ExceptionHandler(ProductCatalogUnavailableException.class)
    public ProblemDetail handleCatalogUnavailable(ProductCatalogUnavailableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        problem.setTitle("Product catalogue unavailable");
        problem.setType(TYPE_BASE.resolve("product-catalog-unavailable"));
        problem.setProperty("productId", exception.getProductId());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationFailure(MethodArgumentNotValidException exception) {
        Map<String, String> violations = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            violations.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "The request body failed validation");
        problem.setTitle("Validation failed");
        problem.setType(TYPE_BASE.resolve("validation-failed"));
        problem.setProperty("violations", violations);
        return problem;
    }
}
