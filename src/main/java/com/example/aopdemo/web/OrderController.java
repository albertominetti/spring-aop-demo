package com.example.aopdemo.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.aopdemo.annotation.Audited;
import com.example.aopdemo.order.Order;
import com.example.aopdemo.order.OrderService;

/**
 * REST layer for the demo.
 *
 * <p>The <b>class</b> is annotated with {@code @Audited}: the
 * {@code @within(Audited)} pointcut in {@code AuditAspect} therefore matches
 * <i>every</i> method of this controller — an "audit the whole HTTP layer"
 * scenario. Contrast this with {@code OrderService}, where {@code @Audited} is
 * placed on individual methods and matched with {@code @annotation(Audited)}.
 */
@RestController
@RequestMapping("/orders")
@Audited
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody CreateOrderRequest request) {
        Order order = orderService.create(request.product(), request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{id}")
    public Order get(@PathVariable String id) {
        return orderService.get(id);
    }

    /** Always fails — used to demonstrate {@code @AfterThrowing} + exception translation. */
    @GetMapping("/{id}/fail")
    public void fail(@PathVariable String id) {
        orderService.failOnDemand(id);
    }

    public record CreateOrderRequest(String product, int quantity) {
    }
}
