package org.example.alert.alert_rules;

import org.example.alert.Alert;
import org.example.alert.AlertSeverity;
import org.example.alert.AlertType;
import org.example.crop.CropProfile;
import org.example.sensor.SensorReading;

import java.util.Optional;

public class SensorFailureRule implements AlertRule {
    @Override
    public Optional<Alert> check(SensorReading reading, CropProfile profile) {
        if (Double.isFinite(reading.value())) {
            return Optional.empty();
        }
        return Optional.of(AlertChecks.createAlert(reading, AlertType.SENSOR_FAILURE,
                AlertSeverity.CRITICAL, reading.type().getLabel() + " sensor reported an invalid reading"));
    }
}
