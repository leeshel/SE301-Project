package org.example.usage;

public record CropUsageRates(double highTempEnergyKwh, double lowTempEnergyKwh,
                             double lowHumidityWaterLitres) {
    public static final CropUsageRates NONE = new CropUsageRates(0, 0, 0);
}
