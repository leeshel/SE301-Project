package org.example.report.report_types;

import org.example.report.Report;
import org.example.report.ReportSample;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class ReportLines {
    private ReportLines() {
    }

    static List<String> buildHeader(String title, List<ReportSample> samples) {
        List<String> lines = new ArrayList<>();
        lines.add("===== " + title + " =====");
        lines.add("Readings included: " + samples.size());
        return lines;
    }

    static List<String> buildUsageReport(String title, List<ReportSample> samples,
                                         Function<ReportSample, Double> value, String unit) {
        List<String> lines = buildHeader(title + " Report", samples);
        double total = samples.stream().mapToDouble(value::apply).sum();
        lines.add(String.format("Total: %.2f %s", total, unit));
        lines.add("Samples: " + samples.size());
        return lines;
    }

    static double average(List<ReportSample> samples, Function<ReportSample, Double> value) {
        return samples.stream().mapToDouble(value::apply).average().orElse(0);
    }
}
