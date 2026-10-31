package org.example.alert.alert_rules;

import org.example.alert.Alert;
import org.example.alert.AlertType;
import org.example.crop.CropProfile;
import org.example.sensor.SensorReading;
import org.example.sensor.SensorType;

import java.util.Optional;

public class LowLightRule implements AlertRule {
    private static final double MINIMUM_LUX = 500;

    @Override
    public Optional<Alert> check(SensorReading reading, CropProfile profile) {
        if (reading.type() != SensorType.LIGHT_LEVEL) {
            return Optional.empty();
        }
        return AlertChecks.checkFixedMinimum(reading, MINIMUM_LUX,
                AlertType.LOW_LIGHT, "Low Light Level");
    }
}
