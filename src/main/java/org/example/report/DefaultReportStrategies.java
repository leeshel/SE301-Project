package org.example.report;

import java.util.List;

public final class DefaultReportStrategies {
    private DefaultReportStrategies() {
    }

    public static List<ReportStrategy> create() {
        return List.of(
                new EnvironmentalReportStrategy(),
                new CropReportStrategy(),
                new WaterUsageReportStrategy(),
                new EnergyConsumptionReportStrategy(),
                new NutrientConsumptionReportStrategy());
    }
}
