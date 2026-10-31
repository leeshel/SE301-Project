package org.example.report;

import java.util.List;

public class NutrientConsumptionReportStrategy implements ReportStrategy {
    @Override
    public ReportType getType() {
        return ReportType.NUTRIENT_CONSUMPTION;
    }

    @Override
    public List<String> buildLines(List<ReportSample> samples) {
        return ReportLines.buildUsageReport("Nutrient Consumption", samples, ReportSample::nutrientsUsedPercent, "%");
    }
}
