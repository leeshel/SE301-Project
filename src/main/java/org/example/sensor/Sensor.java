package org.example.sensor;

public interface Sensor {
    SensorType getType();

    double read();
}
