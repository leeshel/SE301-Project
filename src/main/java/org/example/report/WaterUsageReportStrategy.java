package org.example.report;

import java.util.List;

public class WaterUsageReportStrategy implements ReportStrategy {
    @Override
    public ReportType getType() {
        return ReportType.WATER_USAGE;
    }

    @Override
    public List<String> buildLines(List<ReportSample> samples) {
        return ReportLines.buildUsageReport("Water Usage", samples, ReportSample::waterLitres, "L");
    }
}
