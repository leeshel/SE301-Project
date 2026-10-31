package org.example.sensor;

public record SensorReading(String chamber, String crop, SensorType type, double value) {
}
