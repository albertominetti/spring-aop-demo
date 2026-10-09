package com.example.aopdemo.timing;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

/**
 * In-memory store for the durations measured by {@code TimingAspect}.
 *
 * <p>Kept separate from the aspect itself so that the aspect stays a pure
 * piece of cross-cutting logic and the recorded data can be asserted in tests
 * (and shown by the self-invocation demo endpoint).
 */
@Component
public class TimingRecorder {

    private final Map<String, List<Long>> durationsByMethod = new ConcurrentHashMap<>();

    /**
     * Records the duration (in nanoseconds) of one method invocation.
     *
     * @param key     {@code SimpleClassName#methodName}
     * @param nanos   measured duration
     */
    public void record(String key, long nanos) {
        durationsByMethod.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(nanos);
    }

    /** All recorded durations (nanos) for the given key, in call order. */
    public List<Long> durationsFor(String key) {
        return List.copyOf(durationsByMethod.getOrDefault(key, List.of()));
    }

    /** How many invocations of the given key have been measured so far. */
    public int countFor(String key) {
        return durationsByMethod.getOrDefault(key, List.of()).size();
    }

    public void reset() {
        durationsByMethod.clear();
    }
}
