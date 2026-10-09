package com.example.aopdemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.aopdemo.aspect.ExceptionAspect;
import com.example.aopdemo.order.OrderNotFoundException;
import com.example.aopdemo.order.OrderProcessingException;
import com.example.aopdemo.order.OrderService;

/**
 * Asserts that {@code ExceptionAspect} ({@code @AfterThrowing} + the
 * {@code execution(...)} pointcut) fires, and that the exception it observes
 * is the <i>translated</i> one produced by {@code ExceptionTranslationAspect}
 * (lower precedence = closer to the target method).
 */
@SpringBootTest
class ExceptionAspectTest {

    @Autowired
    private OrderService orderService;

    private Logger aspectLogger;
    private ListAppender<ILoggingEvent> capturedLogs;

    @BeforeEach
    void captureAspectLogs() {
        aspectLogger = (Logger) LoggerFactory.getLogger(ExceptionAspect.class);
        capturedLogs = new ListAppender<>();
        capturedLogs.start();
        aspectLogger.addAppender(capturedLogs);
    }

    @AfterEach
    void stopCapturing() {
        aspectLogger.detachAppender(capturedLogs);
    }

    @Test
    @DisplayName("@AfterThrowing logs the translated exception of failOnDemand")
    void logsTranslatedException() {
        assertThatThrownBy(() -> orderService.failOnDemand("abc"))
                .isInstanceOf(OrderProcessingException.class);

        assertThat(messagesAt(Level.ERROR))
                .anySatisfy(message -> assertThat(message)
                        .contains("OrderService#failOnDemand")
                        .contains("OrderProcessingException"));
    }

    @Test
    @DisplayName("@AfterThrowing also logs plain domain exceptions such as OrderNotFoundException")
    void logsDomainException() {
        assertThatThrownBy(() -> orderService.get("missing-id"))
                .isInstanceOf(OrderNotFoundException.class);

        assertThat(messagesAt(Level.ERROR))
                .anySatisfy(message -> assertThat(message)
                        .contains("OrderService#get")
                        .contains("OrderNotFoundException"));
    }

    private java.util.List<String> messagesAt(Level level) {
        return capturedLogs.list.stream()
                .filter(event -> event.getLevel().equals(level))
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }
}
