package org.example.sensor;

import java.util.EnumMap;
import java.util.Map;

public class ChamberState {
    private final Map<SensorType, Double> values = new EnumMap<>(SensorType.class);

    public ChamberState() {
        for (SensorType type : SensorType.values()) {
            values.put(type, type.getInitialValue());
        }
    }

    // Locks on this object so DeviceController can hold the same lock across a read and a write.
    public double getValue(SensorType type) {
        synchronized (this) {
            return values.get(type);
        }
    }

    public void setValue(SensorType type, double value) {
        synchronized (this) {
            values.put(type, value);
        }
    }
}
