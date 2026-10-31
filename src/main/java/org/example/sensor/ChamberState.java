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

    public synchronized double getValue(SensorType type) {
        return values.get(type);
    }

    public synchronized void setValue(SensorType type, double value) {
        values.put(type, value);
    }
}
