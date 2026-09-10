package com.jessicasales.microservices.product.web;

import com.jessicasales.microservices.product.service.DuplicateProductNameException;
import com.jessicasales.microservices.product.service.ProductNotFoundException;
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
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final URI TYPE_BASE = URI.create("https://api.jessicasales.dev/problems/");

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Product not found");
        problem.setType(TYPE_BASE.resolve("product-not-found"));
        problem.setProperty("productId", exception.getProductId());
        return problem;
    }

    @ExceptionHandler(DuplicateProductNameException.class)
    public ProblemDetail handleDuplicateName(DuplicateProductNameException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Duplicate product name");
        problem.setType(TYPE_BASE.resolve("duplicate-product-name"));
        problem.setProperty("name", exception.getName());
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
