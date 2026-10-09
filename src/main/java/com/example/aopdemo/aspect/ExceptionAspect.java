package com.example.aopdemo.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * ADVICE TYPE: {@code @AfterThrowing} — runs only when the target method
 * throws; it receives the exception but <b>cannot replace it</b>. If you need
 * to translate or swallow an exception you must use {@code @Around} (see
 * {@link ExceptionTranslationAspect}).
 *
 * <p>POINTCUT DESIGNATOR: {@code execution(* package..*(..))} — the classic
 * method-signature pointcut: any return type, any class in the package, any
 * parameters. Compare with {@code within(...)} in {@link ServiceLayerAspect},
 * which selects by declaring type only.
 *
 * <p>{@code @Order(4)}: sits <i>outside</i> {@link ExceptionTranslationAspect}
 * ({@code @Order(5)}), so it logs the exception <b>after translation</b> —
 * here a raw {@link IllegalStateException} has already become an
 * {@code OrderProcessingException}.
 */
@Aspect
@Component
@Order(4)
public class ExceptionAspect {

    private static final Logger log = LoggerFactory.getLogger(ExceptionAspect.class);

    @AfterThrowing(pointcut = "execution(* com.example.aopdemo.order..*(..))", throwing = "error")
    public void logUnhandled(JoinPoint joinPoint, Throwable error) {
        log.error("Unhandled exception in {}#{}: {}",
                joinPoint.getSignature().getDeclaringType().getSimpleName(),
                joinPoint.getSignature().getName(),
                error.toString());
    }
}
