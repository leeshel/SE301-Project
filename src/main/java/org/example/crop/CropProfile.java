package org.example.crop;

import java.util.Optional;

public final class CropProfile {
    private final String name;
    private final Range temperatureRange;
    private final Range humidityRange;
    private final Range soilMoistureRange;
    private final Range lightLevelRange;

    CropProfile(String name, Range temperatureRange, Range humidityRange,
                Range soilMoistureRange, Range lightLevelRange) {
        this.name = name;
        this.temperatureRange = temperatureRange;
        this.humidityRange = humidityRange;
        this.soilMoistureRange = soilMoistureRange;
        this.lightLevelRange = lightLevelRange;
    }

    public String getName() {
        return name;
    }

    public Optional<Range> getTemperatureRange() {
        return Optional.ofNullable(temperatureRange);
    }

    public Optional<Range> getHumidityRange() {
        return Optional.ofNullable(humidityRange);
    }

    public Optional<Range> getSoilMoistureRange() {
        return Optional.ofNullable(soilMoistureRange);
    }

    public Optional<Range> getLightLevelRange() {
        return Optional.ofNullable(lightLevelRange);
    }
}
