package org.example.alert.alert_rules;

import org.example.alert.Alert;
import org.example.crop.CropProfile;
import org.example.sensor.SensorReading;

import java.util.Optional;

public interface AlertRule {
    // The profile is null when the crop is unknown, so rules that need crop limits do not fire.
    Optional<Alert> check(SensorReading reading, CropProfile profile);
}
