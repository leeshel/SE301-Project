package org.example.report.report_types;

import org.example.report.ReportSample;
import org.example.report.ReportType;

import java.util.List;

public class WaterUsageReport implements ReportStrategy {
    @Override
    public ReportType getType() {
        return ReportType.WATER_USAGE;
    }

    @Override
    public List<String> buildLines(List<ReportSample> samples) {
        return ReportLines.buildUsageReport("Water Usage", samples, ReportSample::waterLitres, "L");
    }
}
