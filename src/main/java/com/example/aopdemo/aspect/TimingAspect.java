package com.example.aopdemo.aspect;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.aopdemo.annotation.LogExecutionTime;
import com.example.aopdemo.timing.TimingRecorder;

/**
 * ADVICE TYPE: {@code @Around} — the most powerful advice: it can inspect the
 * arguments, run logic before and after the target method, use the return
 * value, swallow or translate exceptions, and even skip the call entirely.
 *
 * <p>POINTCUT DESIGNATOR: {@code @annotation(...)} — matches only methods that
 * carry the {@link LogExecutionTime} annotation.
 *
 * <p>What it does:
 * <ol>
 *   <li>logs entry with the call arguments,</li>
 *   <li>{@code proceed()} — actually invokes the target method (skipping this
 *       call would short-circuit the business logic!),</li>
 *   <li>logs exit with the return value,</li>
 *   <li>records the elapsed time in {@link TimingRecorder} (in a
 *       {@code finally} block, so failures are measured too).</li>
 * </ol>
 *
 * <p>{@code @Order(1)}: lowest value = highest precedence = this aspect sits
 * <b>outermost</b> in the advice chain, so it measures the time spent inside
 * all the other aspects as well.
 */
@Aspect
@Component
@Order(1)
public class TimingAspect {

    private static final Logger log = LoggerFactory.getLogger(TimingAspect.class);

    private final TimingRecorder recorder;

    public TimingAspect(TimingRecorder recorder) {
        this.recorder = recorder;
    }

    @Around("@annotation(logExecutionTime)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint, LogExecutionTime logExecutionTime) throws Throwable {
        String method = methodKey(joinPoint);
        Object[] args = joinPoint.getArgs();

        log.debug(">> ENTER {}({})", method, Arrays.toString(args));

        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            log.debug("<< EXIT  {} returned {}", method, result);
            return result;
        } finally {
            long elapsedNanos = System.nanoTime() - start;
            recorder.record(method, elapsedNanos);
            log.info("~ {} took {} ms", method, elapsedNanos / 1_000_000.0);
        }
    }

    static String methodKey(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "#" + signature.getName();
    }
}
