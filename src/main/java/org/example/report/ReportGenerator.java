package org.example.report;

import java.time.Instant;
import java.util.Collection;

public interface ReportGenerator {
    Report generate(ReportType type, Collection<ReportSample> samples,
                    Instant periodStart, Instant periodEnd);
}
