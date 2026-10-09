package com.example.aopdemo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method or a whole type as "audited": every call is recorded in an
 * in-memory audit trail ({@code AuditTrailStore}).
 *
 * <p>Because the annotation can be placed on a <b>type</b> as well as on a
 * <b>method</b>, it is used to demonstrate the difference between two
 * pointcut designators (see {@code AuditAspect}):
 *
 * <ul>
 *   <li>{@code @annotation(Audited)} &rarr; matches only methods that are
 *       <i>individually</i> annotated with {@code @Audited};</li>
 *   <li>{@code @within(Audited)} &rarr; matches <i>every</i> method of a class
 *       annotated with {@code @Audited} (class-level annotation).</li>
 * </ul>
 *
 * @see com.example.aopdemo.aspect.AuditAspect
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
}
