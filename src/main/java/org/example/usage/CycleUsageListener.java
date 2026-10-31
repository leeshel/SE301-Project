package org.example.usage;

import org.example.sensor.ReadingListener;
import org.example.sensor.SensorReading;
import org.example.sensor.SensorType;

import java.util.EnumSet;
import java.util.Set;

// Create one per chamber: it records the fixed usage once all sensors have reported.
public class CycleUsageListener implements ReadingListener {
    private final UsageTracker usage;
    private final Set<SensorType> reported = EnumSet.noneOf(SensorType.class);

    public CycleUsageListener(UsageTracker usage) {
        this.usage = usage;
    }

    @Override
    public void onReading(SensorReading reading) {
        reported.add(reading.type());
        if (reported.size() == SensorType.values().length) {
            usage.recordCycle(reading.chamber());
            reported.clear();
        }
    }
}
