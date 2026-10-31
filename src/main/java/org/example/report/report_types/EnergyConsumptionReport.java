package org.example.report.report_types;

import org.example.report.ReportSample;
import org.example.report.ReportType;

import java.util.List;

public class EnergyConsumptionReport implements ReportStrategy {
    @Override
    public ReportType getType() {
        return ReportType.ENERGY_CONSUMPTION;
    }

    @Override
    public List<String> buildLines(List<ReportSample> samples) {
        return ReportLines.buildUsageReport("Energy Consumption", samples, ReportSample::energyKwh, "kWh");
    }
}
