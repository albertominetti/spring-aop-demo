package com.example.aopdemo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.aopdemo.order.OrderService;
import com.example.aopdemo.timing.TimingRecorder;

/**
 * The classic Spring AOP gotcha: a {@code this.method()} self-invocation
 * inside the same bean does <b>not</b> go through the proxy, so no advice
 * runs — even though the method carries {@code @LogExecutionTime}.
 */
@SpringBootTest
class SelfInvocationAspectTest {

    private static final String TRACED = "OrderService#traceOperation";

    @Autowired
    private OrderService orderService;

    @Autowired
    private TimingRecorder timingRecorder;

    @Test
    @DisplayName("aspect runs when the method is called through the proxy")
    void aspectRunsThroughProxy() {
        int before = timingRecorder.countFor(TRACED);

        orderService.traceOperation();

        assertThat(timingRecorder.countFor(TRACED)).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("aspect is bypassed by a this.method() self-invocation")
    void aspectBypassedBySelfInvocation() {
        int before = timingRecorder.countFor(TRACED);

        String result = orderService.callInternally(); // internally: this.traceOperation()

        assertThat(result).isEqualTo("traced-operation-result");
        assertThat(timingRecorder.countFor(TRACED)).isEqualTo(before);
    }

    @Test
    @DisplayName("aspect runs again when the self-call goes through an injected proxy")
    void aspectRunsThroughInjectedSelfProxy() {
        int before = timingRecorder.countFor(TRACED);

        orderService.callViaSelfProxy();

        assertThat(timingRecorder.countFor(TRACED)).isEqualTo(before + 1);
    }
}
