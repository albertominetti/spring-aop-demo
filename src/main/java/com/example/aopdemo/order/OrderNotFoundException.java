package com.example.aopdemo.order;

/**
 * Thrown when an order id does not exist; mapped to HTTP 404.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String id) {
        super("Order not found: " + id);
    }
}
