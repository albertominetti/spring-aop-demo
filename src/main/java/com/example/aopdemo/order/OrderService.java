package com.example.aopdemo.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.example.aopdemo.annotation.Audited;
import com.example.aopdemo.annotation.LogExecutionTime;

/**
 * Business logic with real methods for the aspects to wrap.
 *
 * <p>Annotations used here:
 * <ul>
 *   <li>{@code @LogExecutionTime} &rarr; picked up by {@code TimingAspect}
 *       ({@code @Around} + {@code @annotation(...)})</li>
 *   <li>{@code @Audited} on individual methods &rarr; picked up by
 *       {@code AuditAspect} ({@code @AfterReturning}/{@code @AfterThrowing} +
 *       {@code @annotation(...)})</li>
 * </ul>
 *
 * <p>GOTCHA (self-invocation): {@link #callInternally()} invokes
 * {@link #tracedOperation()} on {@code this} — not through the Spring proxy —
 * so the timing aspect does <b>not</b> run. The only way to reach the proxy
 * from inside the bean is to inject the proxy itself, which is what
 * {@link #selfProxy} does (see the {@code /demo/self-invocation} endpoint).
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository repository;

    /**
     * Self-injection of the <i>proxyed</i> bean. Spring resolves this to the
     * proxy (not the raw target), so calling {@code selfProxy.traceOperation()}
     * goes through the advice chain — in contrast to a plain
     * {@code this.traceOperation()} call.
     */
    private final OrderService selfProxy;

    @Autowired
    public OrderService(OrderRepository repository, @Lazy OrderService selfProxy) {
        this.repository = repository;
        this.selfProxy = selfProxy;
    }

    @LogExecutionTime
    @Audited
    public Order create(String product, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be >= 1");
        }
        return repository.generateAndSave(product, quantity);
    }

    @LogExecutionTime
    @Audited
    public Order get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    /**
     * Always fails: the raw {@link IllegalStateException} it throws is
     * translated to {@link OrderProcessingException} by
     * {@code ExceptionTranslationAspect}, and every aspect outside of it
     * observes the translated exception.
     */
    @LogExecutionTime
    @Audited
    public void failOnDemand(String id) {
        log.debug("simulating a storage failure for order {}", id);
        throw new IllegalStateException("simulated storage failure for order " + id);
    }

    // ---------------------------------------------------------------------
    // Self-invocation demo
    // ---------------------------------------------------------------------

    /** Advised: calling it through the proxy records a duration. */
    @LogExecutionTime
    public String traceOperation() {
        return "traced-operation-result";
    }

    /**
     * NOT annotated itself; it calls {@link #traceOperation()} internally.
     * Because the call is a {@code this.traceOperation()} self-invocation, the
     * timing aspect is bypassed — no duration is recorded.
     */
    public String callInternally() {
        return traceOperation(); // == this.traceOperation() -> proxy bypassed
    }

    /** Same call, but explicitly through the injected proxy -> aspect runs. */
    public String callViaSelfProxy() {
        return selfProxy.traceOperation();
    }
}
