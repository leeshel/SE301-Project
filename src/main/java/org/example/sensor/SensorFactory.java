package org.example.sensor;

import java.util.List;
import java.util.Random;

public final class SensorFactory {
    private SensorFactory() {
    }

    // Keeps the legacy read order, which determines the order of alerts and control requests.
    public static List<Sensor> createChamberSensors(Random random) {
        return List.of(
                new SimulatedSensor(SensorType.TEMPERATURE, 10, 30, random),
                new SimulatedSensor(SensorType.HUMIDITY, 40, 60, random),
                new SimulatedSensor(SensorType.SOIL_MOISTURE, 20, 60, random),
                new SimulatedSensor(SensorType.LIGHT_LEVEL, 300, 900, random),
                new SimulatedSensor(SensorType.NUTRIENT_LEVEL, 20, 80, random));
    }
}
