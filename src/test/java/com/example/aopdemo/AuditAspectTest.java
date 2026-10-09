package com.example.aopdemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.aopdemo.audit.AuditEntry;
import com.example.aopdemo.audit.AuditTrailStore;
import com.example.aopdemo.order.Order;
import com.example.aopdemo.order.OrderProcessingException;
import com.example.aopdemo.order.OrderService;

/**
 * Asserts the behaviour of {@code AuditAspect}: {@code @AfterReturning} and
 * {@code @AfterThrowing} bound to {@code @annotation(Audited)} (the
 * {@code @within(Audited)} part is asserted in {@code OrderControllerTest},
 * where the annotated controller is invoked over HTTP).
 */
@SpringBootTest
class AuditAspectTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AuditTrailStore auditTrailStore;

    @Test
    @DisplayName("@AfterReturning writes an OK entry after a successful call")
    void auditEntryAfterSuccessfulCall() {
        int before = countEntries("OrderService#create", "METHOD", "OK");

        Order order = orderService.create("widget", 3);

        assertThat(countEntries("OrderService#create", "METHOD", "OK")).isEqualTo(before + 1);
        AuditEntry entry = lastEntry("OrderService#create", "METHOD", "OK");
        assertThat(entry.args()).contains("widget").contains("3");
        assertThat(entry.detail()).contains(order.id());
    }

    @Test
    @DisplayName("@AfterThrowing writes a FAILED entry when the audited method throws")
    void auditEntryAfterFailedCall() {
        int before = countEntries("OrderService#failOnDemand", "METHOD", "FAILED");

        assertThatThrownBy(() -> orderService.failOnDemand("boom"))
                .isInstanceOf(OrderProcessingException.class);

        assertThat(countEntries("OrderService#failOnDemand", "METHOD", "FAILED")).isEqualTo(before + 1);
        AuditEntry entry = lastEntry("OrderService#failOnDemand", "METHOD", "FAILED");
        assertThat(entry.detail()).contains("OrderProcessingException");
    }

    private int countEntries(String method, String scope, String outcome) {
        return (int) auditTrailStore.entries().stream()
                .filter(e -> e.method().equals(method) && e.scope().equals(scope) && e.outcome().equals(outcome))
                .count();
    }

    private AuditEntry lastEntry(String method, String scope, String outcome) {
        return auditTrailStore.entries().stream()
                .filter(e -> e.method().equals(method) && e.scope().equals(scope) && e.outcome().equals(outcome))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("no audit entry for " + method));
    }
}
