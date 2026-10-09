package com.example.aopdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Demo application for Spring AOP.
 *
 * <p>Key AOP concepts demonstrated in this code base:
 * <ul>
 *   <li>Custom annotations used as pointcut anchors ({@code @LogExecutionTime}, {@code @Audited})</li>
 *   <li>All four advice types: {@code @Around}, {@code @Before}, {@code @AfterReturning}, {@code @AfterThrowing}</li>
 *   <li>All four pointcut designators: {@code @annotation(...)}, {@code @within(...)},
 *       {@code within(...)} and {@code execution(...)}</li>
 *   <li>Aspect precedence with {@code @Order}</li>
 *   <li>Classic gotchas: self-invocation bypassing the proxy, JDK dynamic proxies vs CGLIB</li>
 * </ul>
 *
 * @see com.example.aopdemo.aspect (the aspects themselves)
 * @see README.md (the written explanation)
 */
@SpringBootApplication
public class AopDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AopDemoApplication.class, args);
    }
}
