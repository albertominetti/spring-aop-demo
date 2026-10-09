package com.example.aopdemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.aopdemo.order.OrderProcessingException;
import com.example.aopdemo.order.OrderService;
import com.example.aopdemo.timing.TimingRecorder;

/**
 * Asserts the behaviour of {@code TimingAspect} ({@code @Around} advice bound
 * to {@code @annotation(LogExecutionTime)}).
 */
@SpringBootTest
class TimingAspectTest {

    private static final String CREATE = "OrderService#create";
    private static final String FAIL = "OrderService#failOnDemand";
    private static final String UNANNOTATED = "OrderService#callInternally";

    @Autowired
    private OrderService orderService;

    @Autowired
    private TimingRecorder timingRecorder;

    @Test
    @DisplayName("records a duration for every annotated method call")
    void recordsDurationForAnnotatedMethod() {
        int before = timingRecorder.countFor(CREATE);

        orderService.create("widget", 2);

        List<Long> durations = timingRecorder.durationsFor(CREATE);
        assertThat(durations).hasSize(before + 1);
        assertThat(durations.get(durations.size() - 1)).isNotNegative();
    }

    @Test
    @DisplayName("records a duration even when the method throws (finally block)")
    void recordsDurationWhenMethodThrows() {
        int before = timingRecorder.countFor(FAIL);

        assertThatThrownBy(() -> orderService.failOnDemand("any"))
                .isInstanceOf(OrderProcessingException.class);

        List<Long> durations = timingRecorder.durationsFor(FAIL);
        assertThat(durations).hasSize(before + 1);
        assertThat(durations.get(durations.size() - 1)).isNotNegative();
    }

    @Test
    @DisplayName("does nothing for methods without @LogExecutionTime")
    void ignoresMethodsWithoutAnnotation() {
        int before = timingRecorder.countFor(UNANNOTATED);

        orderService.callInternally();

        assertThat(timingRecorder.countFor(UNANNOTATED)).isEqualTo(before);
    }
}
