package com.example.aopdemo.order;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

/**
 * Minimal in-memory repository. Real applications would use JPA/JDBC here;
 * keeping it in memory makes the AOP behaviour easy to observe in tests.
 */
@Repository
public class OrderRepository {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public Order save(Order order) {
        orders.put(order.id(), order);
        return order;
    }

    public Optional<Order> findById(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    public Order generateAndSave(String product, int quantity) {
        Order order = Order.created(UUID.randomUUID().toString(), product, quantity);
        return save(order);
    }
}
