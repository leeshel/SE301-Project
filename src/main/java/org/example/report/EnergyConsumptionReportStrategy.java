package org.example.report;

import java.util.List;

public class EnergyConsumptionReportStrategy implements ReportStrategy {
    @Override
    public ReportType getType() {
        return ReportType.ENERGY_CONSUMPTION;
    }

    @Override
    public List<String> buildLines(List<ReportSample> samples) {
        return ReportLines.buildUsageReport("Energy Consumption", samples, ReportSample::energyKwh, "kWh");
    }
}
