package com.example.aopdemo.aspect;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.aopdemo.annotation.Audited;
import com.example.aopdemo.audit.AuditEntry;
import com.example.aopdemo.audit.AuditTrailStore;

/**
 * Demonstrates three advice types on one aspect and shows the difference
 * between the {@code @annotation} and {@code @within} pointcut designators:
 *
 * <ul>
 *   <li>{@code @Before("@within(com.example.aopdemo.annotation.Audited)")} — class-level annotation:
 *       fires for <b>every</b> method of a class annotated with
 *       {@link Audited} (here: {@code OrderController}). Records an
 *       {@code ENTERED} entry with scope {@code CLASS}.</li>
 *   <li>{@code @AfterReturning("@annotation(com.example.aopdemo.annotation.Audited)")} — method-level
 *       annotation: fires only when the annotated method returns normally.
 *       Records an {@code OK} entry with the return value.</li>
 *   <li>{@code @AfterThrowing("@annotation(com.example.aopdemo.annotation.Audited)")} — fires only when the
 *       annotated method throws. Records a {@code FAILED} entry with the
 *       exception.</li>
 * </ul>
 *
 * <p>Note that {@code @AfterReturning} and {@code @AfterThrowing} are
 * <i>mutually exclusive</i>: exactly one of them runs for any given call.
 *
 * <p>GOTCHA: type names inside a pointcut expression string are parsed by
 * AspectJ at runtime, which does <b>not</b> see the {@code import} statements
 * of this source file. That is why {@code Audited} must be written with its
 * fully qualified name here — a simple name would silently match nothing.
 *
 * <p>{@code @Order(3)}: runs inside {@link ServiceLayerAspect}. Because
 * {@link ExceptionTranslationAspect} has a <i>higher</i> order value (lower
 * precedence) and therefore sits closer to the target method, the translated
 * exception is what this aspect observes.
 */
@Aspect
@Component
@Order(3)
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditTrailStore auditTrailStore;

    public AuditAspect(AuditTrailStore auditTrailStore) {
        this.auditTrailStore = auditTrailStore;
    }

    /** {@code @within}: every method of a class annotated with {@code @Audited}. */
    @Before("@within(com.example.aopdemo.annotation.Audited)")
    public void enterAuditedScope(JoinPoint joinPoint) {
        String method = key(joinPoint);
        auditTrailStore.add(new AuditEntry(System.currentTimeMillis(), "CLASS", method,
                Arrays.toString(joinPoint.getArgs()), "ENTERED", "controller entry"));
        log.debug("audit: entered audited scope {}", method);
    }

    /** {@code @annotation}: only methods individually annotated with {@code @Audited}, normal return. */
    @AfterReturning(pointcut = "@annotation(com.example.aopdemo.annotation.Audited)", returning = "result")
    public void auditSuccess(JoinPoint joinPoint, Object result) {
        String method = key(joinPoint);
        auditTrailStore.add(new AuditEntry(System.currentTimeMillis(), "METHOD", method,
                Arrays.toString(joinPoint.getArgs()), "OK", String.valueOf(result)));
        log.debug("audit: {} succeeded with {}", method, result);
    }

    /** {@code @annotation}: only methods individually annotated with {@code @Audited}, exceptional exit. */
    @AfterThrowing(pointcut = "@annotation(com.example.aopdemo.annotation.Audited)", throwing = "error")
    public void auditFailure(JoinPoint joinPoint, Throwable error) {
        String method = key(joinPoint);
        String detail = error + (error.getCause() != null ? " (caused by " + error.getCause() + ")" : "");
        auditTrailStore.add(new AuditEntry(System.currentTimeMillis(), "METHOD", method,
                Arrays.toString(joinPoint.getArgs()), "FAILED", detail));
        log.debug("audit: {} failed with {}", method, detail);
    }

    private static String key(JoinPoint joinPoint) {
        return joinPoint.getSignature().getDeclaringType().getSimpleName() + "#"
                + joinPoint.getSignature().getName();
    }
}
