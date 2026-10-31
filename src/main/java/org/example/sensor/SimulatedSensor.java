package org.example.sensor;

import java.util.Random;

public class SimulatedSensor implements Sensor {
    private final SensorType type;
    private final int minValue;
    private final int valueSpread;
    private final Random random;

    public SimulatedSensor(SensorType type, int minValue, int valueSpread, Random random) {
        this.type = type;
        this.minValue = minValue;
        this.valueSpread = valueSpread;
        this.random = random;
    }

    @Override
    public SensorType getType() {
        return type;
    }

    @Override
    public double read() {
        return minValue + random.nextInt(valueSpread);
    }
}
