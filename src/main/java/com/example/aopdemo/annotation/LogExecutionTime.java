package com.example.aopdemo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method whose execution time should be measured and logged.
 *
 * <p>This annotation is only a <b>marker</b>: it contains no logic.
 * The behaviour lives in {@code TimingAspect}, which binds to it with the
 * pointcut {@code @annotation(LogExecutionTime)}.
 *
 * <p>GOTCHA: the aspect only runs when the call goes through the Spring proxy
 * (i.e. another bean calls the method). A {@code this.method()} self-invocation
 * inside the same class bypasses the proxy, so this annotation is silently ignored.
 *
 * @see com.example.aopdemo.aspect.TimingAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecutionTime {
}
