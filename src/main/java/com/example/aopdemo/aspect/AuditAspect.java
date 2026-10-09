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
import com.example.aopdemo.support.BeanLookup;

/**
 * Demonstrates three advice types on one aspect and shows the difference
 * between the {@code @annotation} and {@code @within} pointcut designators:
 *
 * <ul>
 *   <li>{@code @Before} on {@code @within(...Audited) && execution(...)} — class-level
 *       annotation: fires for <b>every</b> method of a class annotated with
 *       {@link Audited} (here: {@code OrderController}). Records an
 *       {@code ENTERED} entry with scope {@code CLASS}.</li>
 *   <li>{@code @AfterReturning} on {@code @annotation(...Audited) && execution(...)} —
 *       method-level annotation: fires only when the annotated method returns
 *       normally. Records an {@code OK} entry with the return value.</li>
 *   <li>{@code @AfterThrowing} on {@code @annotation(...Audited) && execution(...)} —
 *       fires only when the annotated method throws. Records a {@code FAILED}
 *       entry with the exception.</li>
 * </ul>
 *
 * <p>Every pointcut below is additionally scoped with
 * {@code execution(* *(..))}: under the AspectJ compiler {@code @annotation}
 * and {@code @within} also match method <i>call</i> join points (plus
 * constructors/static initializers for {@code @within}), which would run each
 * advice twice — once at the call site, once at the execution. Spring AOP only
 * supports execution join points, so the conjunct changes nothing in proxy mode.
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

    // No constructor injection: under compile-time weaving the AspectJ runtime —
    // not Spring — instantiates this aspect. Collaborators are looked up per call
    // (see BeanLookup), which is correct in both proxy and weaving mode.

    /** {@code @within}: every method of a class annotated with {@code @Audited}. */
    @Before("@within(com.example.aopdemo.annotation.Audited) && execution(* *(..))")
    public void enterAuditedScope(JoinPoint joinPoint) {
        String method = key(joinPoint);
        BeanLookup.get(AuditTrailStore.class).add(new AuditEntry(System.currentTimeMillis(), "CLASS", method,
                Arrays.toString(joinPoint.getArgs()), "ENTERED", "controller entry"));
        log.debug("audit: entered audited scope {}", method);
    }

    /** {@code @annotation}: only methods individually annotated with {@code @Audited}, normal return. */
    @AfterReturning(pointcut = "@annotation(com.example.aopdemo.annotation.Audited) && execution(* *(..))", returning = "result")
    public void auditSuccess(JoinPoint joinPoint, Object result) {
        String method = key(joinPoint);
        BeanLookup.get(AuditTrailStore.class).add(new AuditEntry(System.currentTimeMillis(), "METHOD", method,
                Arrays.toString(joinPoint.getArgs()), "OK", String.valueOf(result)));
        log.debug("audit: {} succeeded with {}", method, result);
    }

    /** {@code @annotation}: only methods individually annotated with {@code @Audited}, exceptional exit. */
    @AfterThrowing(pointcut = "@annotation(com.example.aopdemo.annotation.Audited) && execution(* *(..))", throwing = "error")
    public void auditFailure(JoinPoint joinPoint, Throwable error) {
        String method = key(joinPoint);
        String detail = error + (error.getCause() != null ? " (caused by " + error.getCause() + ")" : "");
        BeanLookup.get(AuditTrailStore.class).add(new AuditEntry(System.currentTimeMillis(), "METHOD", method,
                Arrays.toString(joinPoint.getArgs()), "FAILED", detail));
        log.debug("audit: {} failed with {}", method, detail);
    }

    private static String key(JoinPoint joinPoint) {
        return joinPoint.getSignature().getDeclaringType().getSimpleName() + "#"
                + joinPoint.getSignature().getName();
    }
}
