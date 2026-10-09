package com.example.aopdemo.order;

/**
 * The domain object this demo revolves around.
 */
public record Order(String id, String product, int quantity, String status) {

    public static Order created(String id, String product, int quantity) {
        return new Order(id, product, quantity, "CREATED");
    }
}
