package com.example.aopdemo.audit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

/**
 * Thread-safe, in-memory audit trail. A real application would write these
 * entries to a database or a log aggregator instead.
 */
@Component
public class AuditTrailStore {

    private final List<AuditEntry> entries = new CopyOnWriteArrayList<>();

    public void add(AuditEntry entry) {
        entries.add(entry);
    }

    public List<AuditEntry> entries() {
        return List.copyOf(entries);
    }

    public void clear() {
        entries.clear();
    }
}
