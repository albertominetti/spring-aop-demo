package com.example.aopdemo.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.aopdemo.order.OrderService;
import com.example.aopdemo.timing.TimingRecorder;

/**
 * Live proof of the <b>self-invocation gotcha</b>.
 *
 * <p>The endpoint invokes {@code OrderService.traceOperation()} twice and
 * compares the recorded durations before/after each call:
 *
 * <ol>
 *   <li><b>directly through the proxy</b> ({@code orderService.traceOperation()})
 *       &rarr; the timing aspect <b>runs</b>;</li>
 *   <li><b>via a self-invocation</b> ({@code orderService.callInternally()},
 *       which internally does {@code this.traceOperation()})
 *       &rarr; the timing aspect is <b>bypassed</b>.</li>
 * </ol>
 *
 * <p>That is why the response reports {@code "aspectApplied": true} for the
 * first call and {@code "aspectApplied": false} for the second — in the
 * default proxy mode. Under compile-time weaving ({@code -Paspectj-ctw})
 * there is no proxy to bypass, so the second call reports {@code true} too.
 */
@RestController
public class SelfInvocationDemoController {

    private static final String TRACED_METHOD = "OrderService#traceOperation";

    private final OrderService orderService;
    private final TimingRecorder timingRecorder;

    public SelfInvocationDemoController(OrderService orderService, TimingRecorder timingRecorder) {
        this.orderService = orderService;
        this.timingRecorder = timingRecorder;
    }

    @GetMapping("/demo/self-invocation")
    public Map<String, Object> selfInvocationDemo() {
        int beforeDirect = timingRecorder.countFor(TRACED_METHOD);
        String direct = orderService.traceOperation();
        boolean directApplied = timingRecorder.countFor(TRACED_METHOD) > beforeDirect;

        int beforeSelf = timingRecorder.countFor(TRACED_METHOD);
        String internal = orderService.callInternally();
        boolean selfInvocationApplied = timingRecorder.countFor(TRACED_METHOD) > beforeSelf;

        int beforeProxy = timingRecorder.countFor(TRACED_METHOD);
        String viaProxy = orderService.callViaSelfProxy();
        boolean selfProxyApplied = timingRecorder.countFor(TRACED_METHOD) > beforeProxy;

        return Map.of(
                "callThroughProxy", Map.of("result", direct, "aspectApplied", directApplied),
                "selfInvocationThisDotCall", Map.of("result", internal, "aspectApplied", selfInvocationApplied,
                        "explanation", "callInternally() invokes traceOperation() on 'this', "
                                + "which bypasses the Spring proxy, so the aspect does not run"),
                "selfInvocationViaInjectedProxy", Map.of("result", viaProxy, "aspectApplied", selfProxyApplied,
                        "explanation", "callViaSelfProxy() invokes traceOperation() on the injected proxy, "
                                + "so the aspect runs"));
    }
}
