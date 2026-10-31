package org.example.alert;

import org.example.crop.CropProfile;
import org.example.crop.Range;
import org.example.sensor.SensorReading;
import org.example.sensor.SensorType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Preserves the legacy thresholds while exposing the modern alert model.
 */
public final class LegacyCompatibleAlertEvaluator
        implements AlertEvaluator, ReadingAlertEvaluator {
    private static final double LEGACY_LIGHT_MINIMUM = 500;
    private static final double LEGACY_NUTRIENT_MINIMUM = 40;

    @Override
    public List<Alert> evaluate(SensorReading reading, CropProfile profile) {
        List<Alert> alerts = new ArrayList<>();
        SensorType type = reading.type();
        if (type == SensorType.TEMPERATURE) {
            addRangeAlert(alerts, reading, profile == null ? null
                    : profile.getTemperatureRange().orElse(null), AlertType.TEMPERATURE);
        } else if (type == SensorType.HUMIDITY) {
            addRangeAlert(alerts, reading, profile == null ? null
                    : profile.getHumidityRange().orElse(null), AlertType.HUMIDITY);
        } else if (type == SensorType.SOIL_MOISTURE) {
            addLowAlert(alerts, reading, profile == null ? null
                    : profile.getSoilMoistureRange().orElse(null));
        } else if (type == SensorType.LIGHT_LEVEL
                && reading.value() < LEGACY_LIGHT_MINIMUM) {
            alerts.add(alert(reading, AlertType.LOW_LIGHT, AlertSeverity.WARNING,
                    "Low Light Level"));
        } else if (type == SensorType.NUTRIENT_LEVEL
                && reading.value() < LEGACY_NUTRIENT_MINIMUM) {
            alerts.add(alert(reading, AlertType.NUTRIENT_DEFICIENCY,
                    AlertSeverity.WARNING, "Low Nutrient Level"));
        }
        return List.copyOf(alerts);
    }

    @Override
    public List<Alert> evaluate(EnvironmentalObservation observation, CropProfile profile) {
        List<Alert> alerts = new ArrayList<>();
        for (Map.Entry<SensorType, Double> entry : observation.values().entrySet()) {
            alerts.addAll(evaluate(new SensorReading(observation.chamber(),
                    observation.crop(), entry.getKey(), entry.getValue()), profile));
        }
        return List.copyOf(alerts);
    }

    private void addRangeAlert(List<Alert> alerts, SensorReading reading,
                               Range range, AlertType type) {
        if (range == null || !Double.isFinite(reading.value())
                || range.min() <= reading.value() && reading.value() <= range.max()) {
            return;
        }
        double distance = reading.value() < range.min()
                ? range.min() - reading.value() : reading.value() - range.max();
        double span = Math.max(range.max() - range.min(), 1);
        AlertSeverity severity = distance >= span * 0.25
                ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
        alerts.add(alert(reading, type, severity, type == AlertType.TEMPERATURE
                ? (reading.value() < range.min() ? "Low Temp" : "High Temp")
                : (reading.value() < range.min() ? "Low Humidity" : "High Humidity")));
    }

    private void addLowAlert(List<Alert> alerts, SensorReading reading, Range range) {
        if (range == null || !Double.isFinite(reading.value())
                || reading.value() >= range.min()) {
            return;
        }
        AlertSeverity severity = reading.value() < range.min() * 0.8
                ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
        alerts.add(alert(reading, AlertType.LOW_SOIL_MOISTURE, severity,
                "Low Soil Moisture"));
    }

    private Alert alert(SensorReading reading, AlertType type,
                        AlertSeverity severity, String message) {
        return new Alert(reading.chamber(), reading.crop(), type, severity,
                message, java.time.Instant.now());
    }
}
