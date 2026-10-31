package org.example.alert.alert_rules;

import org.example.alert.Alert;
import org.example.alert.AlertType;
import org.example.crop.CropProfile;
import org.example.crop.Range;
import org.example.sensor.SensorReading;
import org.example.sensor.SensorType;

import java.util.Optional;

public class HumidityRule implements AlertRule {
    @Override
    public Optional<Alert> check(SensorReading reading, CropProfile profile) {
        if (reading.type() != SensorType.HUMIDITY || profile == null) {
            return Optional.empty();
        }
        Range range = profile.getHumidityRange().orElse(null);
        return AlertChecks.checkRange(reading, range,
                AlertType.HIGH_HUMIDITY, "High Humidity",
                AlertType.LOW_HUMIDITY, "Low Humidity");
    }
}
