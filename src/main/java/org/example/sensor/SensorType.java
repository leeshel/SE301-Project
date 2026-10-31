package org.example.sensor;

public enum SensorType {
    TEMPERATURE("Temperature", 24),
    HUMIDITY("Humidity", 70),
    SOIL_MOISTURE("Soil", 60),
    LIGHT_LEVEL("Light", 600),
    NUTRIENT_LEVEL("Nutrient", 70);

    private final String label;
    private final double initialValue;

    SensorType(String label, double initialValue) {
        this.label = label;
        this.initialValue = initialValue;
    }

    public String getLabel() {
        return label;
    }

    public double getInitialValue() {
        return initialValue;
    }
}
