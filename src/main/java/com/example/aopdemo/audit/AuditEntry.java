package com.example.aopdemo.audit;

/**
 * A single record in the audit trail.
 *
 * @param timestamp when the audited join point was reached
 * @param scope     {@code METHOD} for {@code @annotation(Audited)},
 *                  {@code CLASS} for {@code @within(Audited)}
 * @param method    {@code SimpleClassName#methodName}
 * @param args      string representation of the call arguments
 * @param outcome   {@code OK}, {@code FAILED} or {@code ENTERED}
 * @param detail    result or exception (depending on the outcome)
 */
public record AuditEntry(
        long timestamp,
        String scope,
        String method,
        String args,
        String outcome,
        String detail) {
}
