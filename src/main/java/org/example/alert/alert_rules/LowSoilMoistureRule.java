package org.example.alert.alert_rules;

import org.example.alert.Alert;
import org.example.alert.AlertSeverity;
import org.example.alert.AlertType;
import org.example.crop.CropProfile;
import org.example.crop.Range;
import org.example.sensor.SensorReading;
import org.example.sensor.SensorType;

import java.util.Optional;

public class LowSoilMoistureRule implements AlertRule {
    @Override
    public Optional<Alert> check(SensorReading reading, CropProfile profile) {
        if (reading.type() != SensorType.SOIL_MOISTURE || profile == null) {
            return Optional.empty();
        }
        Range range = profile.getSoilMoistureRange().orElse(null);
        if (range == null || !Double.isFinite(reading.value())
                || reading.value() >= range.min()) {
            return Optional.empty();
        }
        AlertSeverity severity = reading.value() < range.min() * 0.8
                ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
        return Optional.of(AlertChecks.createAlert(reading, AlertType.LOW_SOIL_MOISTURE,
                severity, "Low Soil Moisture"));
    }
}
