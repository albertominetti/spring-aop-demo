package com.example.aopdemo.web;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.aopdemo.order.OrderNotFoundException;
import com.example.aopdemo.order.OrderProcessingException;

/**
 * Maps the exceptions that survive the advice chain to HTTP responses.
 * Aspects are a great place to <i>observe</i> or <i>translate</i> exceptions;
 * turning them into HTTP status codes is still the web layer's job.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(OrderNotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(OrderProcessingException.class)
    public ResponseEntity<Map<String, Object>> processingFailure(OrderProcessingException ex) {
        return body(HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException ex) {
        return body(HttpStatus.BAD_REQUEST, ex);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, RuntimeException ex) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", ex.getMessage()));
    }
}
