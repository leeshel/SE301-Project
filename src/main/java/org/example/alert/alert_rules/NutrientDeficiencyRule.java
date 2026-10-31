package org.example.alert.alert_rules;

import org.example.alert.Alert;
import org.example.alert.AlertType;
import org.example.crop.CropProfile;
import org.example.sensor.SensorReading;
import org.example.sensor.SensorType;

import java.util.Optional;

public class NutrientDeficiencyRule implements AlertRule {
    private static final double MINIMUM_PERCENT = 40;

    @Override
    public Optional<Alert> check(SensorReading reading, CropProfile profile) {
        if (reading.type() != SensorType.NUTRIENT_LEVEL) {
            return Optional.empty();
        }
        return AlertChecks.checkFixedMinimum(reading, MINIMUM_PERCENT,
                AlertType.NUTRIENT_DEFICIENCY, "Low Nutrient Level");
    }
}
