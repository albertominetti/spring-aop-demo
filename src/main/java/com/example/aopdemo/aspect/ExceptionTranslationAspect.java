package com.example.aopdemo.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.aopdemo.order.OrderProcessingException;

/**
 * ADVICE TYPE: {@code @Around} used for <b>exception translation</b> — the one
 * thing {@code @AfterThrowing} cannot do.
 *
 * <p>{@code OrderService.failOnDemand(...)} deliberately throws a raw
 * {@link IllegalStateException} (as a legacy/persistence layer might). This
 * aspect catches it and rethrows a proper domain exception
 * ({@link OrderProcessingException}), which the REST layer then maps to an
 * HTTP 500 with a JSON body.
 *
 * <p>POINTCUT DESIGNATOR: {@code execution(...)} restricted to one concrete
 * method signature (class + method name + parameters).
 *
 * <p>{@code @Order(5)} = lowest precedence = <b>innermost</b> aspect, directly
 * around the target method. That is why every outer aspect
 * ({@link ExceptionAspect}, {@link AuditAspect}, ...) observes the translated
 * {@code OrderProcessingException} instead of the original
 * {@code IllegalStateException}.
 */
@Aspect
@Component
@Order(5)
public class ExceptionTranslationAspect {

    private static final Logger log = LoggerFactory.getLogger(ExceptionTranslationAspect.class);

    @Around("execution(* com.example.aopdemo.order.OrderService.failOnDemand(..))")
    public Object translateStorageFailure(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (IllegalStateException rawFailure) {
            OrderProcessingException translated = new OrderProcessingException(
                    "Order storage failure: " + rawFailure.getMessage(), rawFailure);
            log.debug("translated {} into {}", rawFailure.getClass().getSimpleName(), translated.getClass().getSimpleName());
            throw translated;
        }
    }
}
