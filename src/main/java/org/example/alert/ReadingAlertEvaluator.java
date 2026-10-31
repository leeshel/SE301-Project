package org.example.alert;

import org.example.crop.CropProfile;
import org.example.sensor.SensorReading;

import java.util.List;

@FunctionalInterface
public interface ReadingAlertEvaluator {
    List<Alert> evaluate(SensorReading reading, CropProfile profile);
}
