package org.example.report;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class StandardReportGenerator implements ReportGenerator {
    private final EnumMap<ReportType, Function<List<ReportSample>, List<String>>> strategies =
            new EnumMap<>(ReportType.class);

    public StandardReportGenerator() {
        strategies.put(ReportType.DAILY_ENVIRONMENTAL, this::environmentalReport);
        strategies.put(ReportType.WEEKLY_CROP, this::cropReport);
        strategies.put(ReportType.WATER_USAGE, samples -> usageReport("Water Usage", samples,
                ReportSample::waterLitres, "L"));
        strategies.put(ReportType.ENERGY_CONSUMPTION, samples -> usageReport("Energy Consumption",
                samples, ReportSample::energyKwh, "kWh"));
        strategies.put(ReportType.NUTRIENT_CONSUMPTION, samples -> usageReport(
                "Nutrient Consumption", samples, ReportSample::nutrientsUsedPercent, "%"));
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

        List<ReportSample> selected = samples.stream()
                .filter(sample -> !sample.recordedAt().isBefore(periodStart)
                        && !sample.recordedAt().isAfter(periodEnd))
                .sorted(Comparator.comparing(ReportSample::recordedAt))
                .toList();
        List<String> lines = strategies.get(type).apply(selected);
        return new Report(type, periodStart, periodEnd, Instant.now(), lines);
    }

    private List<String> environmentalReport(List<ReportSample> samples) {
        List<String> lines = header("Daily Environmental Report", samples);
        if (samples.isEmpty()) {
            lines.add("No environmental readings recorded.");
            return lines;
        }
        lines.add(String.format("Average temperature: %.2f °C", average(samples, ReportSample::temperature)));
        lines.add(String.format("Average humidity: %.2f %%", average(samples, ReportSample::humidity)));
        lines.add(String.format("Average soil moisture: %.2f %%", average(samples, ReportSample::soilMoisture)));
        lines.add(String.format("Average light level: %.2f lux", average(samples, ReportSample::lightLevel)));
        lines.add(String.format("Average nutrient level: %.2f %%", average(samples, ReportSample::nutrientLevel)));
        return lines;
    }

    private List<String> cropReport(List<ReportSample> samples) {
        List<String> lines = header("Weekly Crop Report", samples);
        samples.stream().map(ReportSample::crop).distinct().sorted()
                .forEach(crop -> {
                    List<ReportSample> cropSamples = samples.stream()
                            .filter(sample -> sample.crop().equals(crop)).toList();
                    lines.add(String.format("%s: %d readings, average nutrient level %.2f %%",
                            crop, cropSamples.size(), average(cropSamples, ReportSample::nutrientLevel)));
                });
        if (samples.isEmpty()) {
            lines.add("No crop readings recorded.");
        }
        return lines;
    }

    private List<String> usageReport(String title, List<ReportSample> samples,
                                     Function<ReportSample, Double> value, String unit) {
        List<String> lines = header(title + " Report", samples);
        double total = samples.stream().mapToDouble(value::apply).sum();
        lines.add(String.format("Total: %.2f %s", total, unit));
        lines.add("Samples: " + samples.size());
        return lines;
    }

    private List<String> header(String title, List<ReportSample> samples) {
        List<String> lines = new ArrayList<>();
        lines.add("===== " + title + " =====");
        lines.add("Readings included: " + samples.size());
        return lines;
    }

    private double average(List<ReportSample> samples, Function<ReportSample, Double> value) {
        return samples.stream().mapToDouble(value::apply).average().orElse(0);
    }
}
