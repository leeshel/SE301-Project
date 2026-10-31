package org.example.alert;

import java.time.Instant;

public record Alert(
        String chamber,
        String crop,
        AlertType type,
        AlertSeverity severity,
        String message,
        Instant occurredAt) {
}
