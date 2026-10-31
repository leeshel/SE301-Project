package org.example.report.report_types;

import java.util.List;

public final class DefaultReportStrategies {
    private DefaultReportStrategies() {
    }

    public static List<ReportStrategy> create() {
        return List.of(
                new DailyEnvironmentalReport(),
                new WeeklyCropReport(),
                new WaterUsageReport(),
                new EnergyConsumptionReport(),
                new NutrientConsumptionReport());
    }
}
