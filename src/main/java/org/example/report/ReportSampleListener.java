package org.example.report;

import org.example.sensor.ReadingListener;
import org.example.sensor.SensorReading;
import org.example.sensor.SensorType;
import org.example.usage.ChamberUsage;
import org.example.usage.UsageTracker;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

// Create one per chamber: it collects that chamber's readings into one sample per cycle.
public class ReportSampleListener implements ReadingListener {
    private final ReportSampleStore store;
    private final UsageTracker usage;
    private final Map<SensorType, Double> cycleValues = new EnumMap<>(SensorType.class);

    public ReportSampleListener(ReportSampleStore store, UsageTracker usage) {
        this.store = store;
        this.usage = usage;
    }

    @Override
    public void onReading(SensorReading reading) {
        cycleValues.put(reading.type(), reading.value());
        if (cycleValues.size() < SensorType.values().length) {
            return;
        }
        ChamberUsage used = usage.takeChamberUsage(reading.chamber());
        store.add(new ReportSample(
                Instant.now(),
                reading.chamber(),
                reading.crop(),
                cycleValues.get(SensorType.TEMPERATURE),
                cycleValues.get(SensorType.HUMIDITY),
                cycleValues.get(SensorType.SOIL_MOISTURE),
                cycleValues.get(SensorType.LIGHT_LEVEL),
                cycleValues.get(SensorType.NUTRIENT_LEVEL),
                used.waterLitres(),
                used.energyKwh(),
                used.nutrientsPercent()));
        cycleValues.clear();
    }
}
