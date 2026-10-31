package org.example.report.report_types;

import org.example.report.Report;
import org.example.report.ReportSample;
import org.example.report.ReportType;

import java.util.List;

public class WeeklyCropReport implements ReportStrategy {
    @Override
    public ReportType getType() {
        return ReportType.WEEKLY_CROP;
    }

    @Override
    public List<String> buildLines(List<ReportSample> samples) {
        List<String> lines = ReportLines.buildHeader("Weekly Crop Report", samples);
        samples.stream().map(ReportSample::crop).distinct().sorted()
                .forEach(crop -> {
                    List<ReportSample> cropSamples = samples.stream()
                            .filter(sample -> sample.crop().equals(crop)).toList();
                    lines.add(String.format("%s: %d readings, average nutrient level %.2f %%",
                            crop, cropSamples.size(),
                            ReportLines.average(cropSamples, ReportSample::nutrientLevel)));
                });
        if (samples.isEmpty()) {
            lines.add("No crop readings recorded.");
        }
        return lines;
    }
}
