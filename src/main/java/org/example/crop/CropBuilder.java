package org.example.crop;

public class CropBuilder {
    private final String name;
    private Range temperatureRange;
    private Range humidityRange;
    private Range soilMoistureRange;
    private Range lightLevelRange;

    public CropBuilder(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Crop name is required");
        }
        this.name = name;
    }

    public CropBuilder temperatureRange(double min, double max) {
        this.temperatureRange = new Range(min, max);
        return this;
    }

    public CropBuilder humidityRange(double min, double max) {
        this.humidityRange = new Range(min, max);
        return this;
    }

    public CropBuilder soilMoistureRange(double min, double max) {
        this.soilMoistureRange = new Range(min, max);
        return this;
    }

    public CropBuilder lightLevelRange(double min, double max) {
        this.lightLevelRange = new Range(min, max);
        return this;
    }

    public CropProfile build() {
        return new CropProfile(name, temperatureRange, humidityRange, soilMoistureRange, lightLevelRange);
    }
}
