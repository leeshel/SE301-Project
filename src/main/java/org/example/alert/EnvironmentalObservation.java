package org.example.alert;

import org.example.sensor.SensorType;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public record EnvironmentalObservation(
        String chamber,
        String crop,
        Instant observedAt,
        Map<SensorType, Double> values) {

    public EnvironmentalObservation {
        if (chamber == null || chamber.isBlank()) {
            throw new IllegalArgumentException("Chamber is required");
        }
        if (crop == null || crop.isBlank()) {
            throw new IllegalArgumentException("Crop is required");
        }
        Objects.requireNonNull(observedAt, "observedAt");
        EnumMap<SensorType, Double> copiedValues = new EnumMap<>(SensorType.class);
        if (values != null) {
            copiedValues.putAll(values);
        }
        values = Map.copyOf(copiedValues);
    }

    public boolean hasValue(SensorType type) {
        return values.containsKey(type);
    }

    public double valueOf(SensorType type) {
        Double value = values.get(type);
        if (value == null) {
            throw new IllegalArgumentException("No reading for " + type);
        }
        return value;
    }

    public boolean hasInvalidValue(SensorType type) {
        return !hasValue(type) || !Double.isFinite(valueOf(type));
    }
}
