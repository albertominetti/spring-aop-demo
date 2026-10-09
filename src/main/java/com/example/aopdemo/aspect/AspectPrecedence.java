package com.example.aopdemo.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclarePrecedence;

/**
 * Aspect precedence for <b>compile-time weaving mode</b> ({@code -Paspectj-ctw}).
 *
 * <p>{@code @DeclarePrecedence} lists the aspects outermost-first, mirroring
 * the {@code @Order} values used in proxy mode (where Spring — not AspectJ —
 * builds the advice chain, and {@code @Order} rules).
 *
 * <p>Why a separate class? Spring AOP does not support
 * {@code @DeclarePrecedence}: an {@code @Aspect} carrying it is silently
 * skipped during advisor discovery, so putting it on a real aspect (e.g.
 * {@code TimingAspect}) would disable that aspect in proxy mode. Kept in its
 * own class with <b>no</b> {@code @Component}, it is invisible to Spring in
 * proxy mode (never becomes a bean) and is picked up by {@code ajc} from the
 * compiled sources in weaving mode.
 */
@Aspect
@DeclarePrecedence("com.example.aopdemo.aspect.TimingAspect, "
        + "com.example.aopdemo.aspect.ServiceLayerAspect, "
        + "com.example.aopdemo.aspect.AuditAspect, "
        + "com.example.aopdemo.aspect.ExceptionAspect, "
        + "com.example.aopdemo.aspect.ExceptionTranslationAspect")
public class AspectPrecedence {
}
