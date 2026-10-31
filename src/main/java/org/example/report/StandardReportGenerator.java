package org.example.report;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StandardReportGenerator implements ReportGenerator {
    private final Map<ReportType, ReportStrategy> strategies = new EnumMap<>(ReportType.class);

    public StandardReportGenerator() {
        this(DefaultReportStrategies.create());
    }

    public StandardReportGenerator(List<ReportStrategy> strategies) {
        for (ReportStrategy strategy : strategies) {
            this.strategies.put(strategy.getType(), strategy);
        }
    }

    @Override
    public Report generate(ReportType type, Collection<ReportSample> samples,
                            Instant periodStart, Instant periodEnd) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(samples, "samples");
        Objects.requireNonNull(periodStart, "periodStart");
        Objects.requireNonNull(periodEnd, "periodEnd");
        if (periodStart.isAfter(periodEnd)) {
            throw new IllegalArgumentException("periodStart must not be after periodEnd");
        }
        ReportStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No report strategy for " + type);
        }

        List<ReportSample> selected = samples.stream()
                .filter(sample -> !sample.recordedAt().isBefore(periodStart)
                        && !sample.recordedAt().isAfter(periodEnd))
                .sorted(Comparator.comparing(ReportSample::recordedAt))
                .toList();
        return new Report(type, periodStart, periodEnd, Instant.now(), strategy.buildLines(selected));
    }
}
