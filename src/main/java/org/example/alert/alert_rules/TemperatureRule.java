package org.example.alert.alert_rules;

import org.example.alert.Alert;
import org.example.alert.AlertType;
import org.example.crop.CropProfile;
import org.example.crop.Range;
import org.example.sensor.SensorReading;
import org.example.sensor.SensorType;

import java.util.Optional;

public class TemperatureRule implements AlertRule {
    @Override
    public Optional<Alert> check(SensorReading reading, CropProfile profile) {
        if (reading.type() != SensorType.TEMPERATURE || profile == null) {
            return Optional.empty();
        }
        Range range = profile.getTemperatureRange().orElse(null);
        return AlertChecks.checkRange(reading, range,
                AlertType.HIGH_TEMPERATURE, "High Temp",
                AlertType.LOW_TEMPERATURE, "Low Temp");
    }
}
