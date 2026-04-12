package com.shopflow.orders.infrastructure.rest;

import com.shopflow.orders.application.OrderService.OrderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Global exception handler using RFC 7807 ProblemDetail.
 *
 * Generated in T09 with Chat:
 * "Añade un @RestControllerAdvice que maneje OrderNotFoundException con 404,
 * MethodArgumentNotValidException con 400, e IllegalStateException con 409.
 * Usa ProblemDetail (RFC 7807) para todas las respuestas de error.
 * Incluye el traceId del MDC en el detalle cuando esté disponible."
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleNotFound(OrderNotFoundException ex) {
        log.warn("Order not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://shopflow.com/errors/order-not-found"));
        problem.setTitle("Order Not Found");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");

        log.warn("Validation error: {}", detail);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("https://shopflow.com/errors/validation-error"));
        problem.setTitle("Validation Error");
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleBusinessRule(IllegalStateException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://shopflow.com/errors/business-rule-violation"));
        problem.setTitle("Business Rule Violation");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        // Never expose exception details to the client
        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support."
        );
        problem.setType(URI.create("https://shopflow.com/errors/internal-error"));
        problem.setTitle("Internal Server Error");
        return problem;
    }
}
