package org.example.report;

import java.time.Instant;
import java.util.List;

public record Report(
        ReportType type,
        Instant periodStart,
        Instant periodEnd,
        Instant generatedAt,
        List<String> lines) {

    public Report {
        lines = List.copyOf(lines);
    }

    public String asText() {
        return String.join(System.lineSeparator(), lines);
    }
}
