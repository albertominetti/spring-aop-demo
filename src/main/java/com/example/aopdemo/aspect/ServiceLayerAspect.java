package com.example.aopdemo.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * ADVICE TYPE: {@code @Before} — runs before the target method; it cannot
 * change the return value nor prevent the call (a {@code @BeforeReturning},
 * {@code @After*} or {@code @Around} would be needed for that).
 *
 * <p>POINTCUT DESIGNATOR: {@code within(package..*)} — matches every method
 * declared in the given package (and sub-packages). Note the difference from
 * {@code execution(...)}: {@code within()} selects by <b>declaring type</b>,
 * {@code execution()} by method signature (visibility, return type, name,
 * parameters). Here we simply log "some service method is about to run".
 * The {@code execution(* *(..))} conjunct keeps matching to method executions:
 * under the AspectJ compiler {@code within(...)} alone would also match
 * constructors, static initializers and field access.
 *
 * <p>{@code @Order(2)}: runs inside {@link TimingAspect} but outside the
 * audit/exception aspects.
 */
@Aspect
@Component
@Order(2)
public class ServiceLayerAspect {

    private static final Logger log = LoggerFactory.getLogger(ServiceLayerAspect.class);

    @Before("within(com.example.aopdemo.order..*) && execution(* *(..))")
    public void beforeServiceCall(JoinPoint joinPoint) {
        log.debug("-- service layer entering {}#{}", joinPoint.getSignature().getDeclaringType().getSimpleName(),
                joinPoint.getSignature().getName());
    }
}
