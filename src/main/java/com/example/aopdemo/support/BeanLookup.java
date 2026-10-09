package com.example.aopdemo.support;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Static bridge to the Spring context, used by the {@code @Aspect} classes.
 *
 * <p>Why this exists: the aspects must work in <b>two modes</b>.
 * <ul>
 *   <li><b>Proxy mode (default):</b> Spring instantiates each aspect as a bean
 *       and wraps matching beans in proxies. Constructor injection would work here.</li>
 *   <li><b>Compile-time weaving ({@code -Paspectj-ctw} profile):</b> {@code ajc}
 *       weaves the advice into the bytecode and the <i>AspectJ runtime</i>
 *       instantiates the aspects — Spring never calls their constructors, so
 *       injected fields would stay {@code null}.</li>
 * </ul>
 * Looking collaborators up statically (method-local, never cached in fields)
 * is the smallest pattern that is correct in both modes. The {@code @Component}
 * on the aspects themselves is still required: in proxy mode it is how Spring
 * discovers them; in weaving mode the resulting bean simply sits idle
 * ({@code spring.aop.auto=false} creates no advisors for it).
 */
@Component
public class BeanLookup implements ApplicationContextAware {

    private static volatile ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static <T> T get(Class<T> type) {
        ApplicationContext ctx = context;
        if (ctx == null) {
            throw new IllegalStateException("ApplicationContext not initialized yet");
        }
        return ctx.getBean(type);
    }
}
