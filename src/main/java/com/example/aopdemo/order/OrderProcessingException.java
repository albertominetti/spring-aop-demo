package com.example.aopdemo.order;

/**
 * Domain-level failure; mapped to HTTP 500.
 *
 * <p>{@code ExceptionTranslationAspect} produces this exception by translating
 * the raw {@link IllegalStateException} thrown by
 * {@link OrderService#failOnDemand(String)}.
 */
public class OrderProcessingException extends RuntimeException {

    public OrderProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
