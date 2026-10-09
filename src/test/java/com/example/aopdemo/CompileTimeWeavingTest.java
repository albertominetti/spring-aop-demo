package com.example.aopdemo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.aopdemo.order.OrderService;
import com.example.aopdemo.timing.TimingRecorder;
import com.example.aopdemo.web.OrderController;

/**
 * Proves that compile-time weaving mode ({@code mvn -Paspectj-ctw ...}, which
 * sets {@code spring.aop.auto=false} for the test JVM) really weaves the
 * advice into the bytecode instead of proxying:
 *
 * <ul>
 *   <li>beans are plain instances, not Spring AOP proxies;</li>
 *   <li>a {@code this.traceOperation()} self-invocation <b>is</b> advised —
 *       the exact opposite of proxy mode (see {@code SelfInvocationAspectTest},
 *       whose bypass test is disabled in this mode).</li>
 * </ul>
 *
 * <p>Every other test in the suite runs unmodified in <i>both</i> modes; their
 * "exactly one more record" assertions double as a guard against advice
 * running twice (woven + proxied).
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "spring.aop.auto", matches = "false")
class CompileTimeWeavingTest {

    private static final String TRACED = "OrderService#traceOperation";

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderController orderController;

    @Autowired
    private TimingRecorder timingRecorder;

    @Test
    @DisplayName("beans are not proxied when spring.aop.auto=false")
    void beansAreNotProxies() {
        assertThat(AopUtils.isAopProxy(orderService)).isFalse();
        assertThat(AopUtils.isAopProxy(orderController)).isFalse();
    }

    @Test
    @DisplayName("self-invocation IS advised with compile-time weaving")
    void selfInvocationIsAdvised() {
        int before = timingRecorder.countFor(TRACED);

        String result = orderService.callInternally(); // internally: this.traceOperation()

        assertThat(result).isEqualTo("traced-operation-result");
        assertThat(timingRecorder.countFor(TRACED)).isEqualTo(before + 1);
    }
}
