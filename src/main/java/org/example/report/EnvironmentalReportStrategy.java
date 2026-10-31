package org.example.report;

import java.util.List;

public class EnvironmentalReportStrategy implements ReportStrategy {
    @Override
    public ReportType getType() {
        return ReportType.DAILY_ENVIRONMENTAL;
    }

    @Override
    public List<String> buildLines(List<ReportSample> samples) {
        List<String> lines = ReportLines.buildHeader("Daily Environmental Report", samples);
        if (samples.isEmpty()) {
            lines.add("No environmental readings recorded.");
            return lines;
        }
        lines.add(String.format("Average temperature: %.2f °C", ReportLines.average(samples, ReportSample::temperature)));
        lines.add(String.format("Average humidity: %.2f %%", ReportLines.average(samples, ReportSample::humidity)));
        lines.add(String.format("Average soil moisture: %.2f %%", ReportLines.average(samples, ReportSample::soilMoisture)));
        lines.add(String.format("Average light level: %.2f lux", ReportLines.average(samples, ReportSample::lightLevel)));
        lines.add(String.format("Average nutrient level: %.2f %%", ReportLines.average(samples, ReportSample::nutrientLevel)));
        return lines;
    }
}
