package org.example.report.report_types;

import org.example.report.ReportSample;
import org.example.report.ReportType;

import java.util.List;

public class NutrientConsumptionReport implements ReportStrategy {
    @Override
    public ReportType getType() {
        return ReportType.NUTRIENT_CONSUMPTION;
    }

    @Override
    public List<String> buildLines(List<ReportSample> samples) {
        return ReportLines.buildUsageReport("Nutrient Consumption", samples, ReportSample::nutrientsUsedPercent, "%");
    }
}
