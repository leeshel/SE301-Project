package org.example.report;

import java.time.Instant;
import java.util.Objects;

public record ReportSample(
        Instant recordedAt,
        String chamber,
        String crop,
        double temperature,
        double humidity,
        double soilMoisture,
        double lightLevel,
        double nutrientLevel,
        double waterLitres,
        double energyKwh,
        double nutrientsUsedPercent) {

    public ReportSample {
        Objects.requireNonNull(recordedAt, "recordedAt");
        if (chamber == null || chamber.isBlank() || crop == null || crop.isBlank()) {
            throw new IllegalArgumentException("Chamber and crop are required");
        }
        validateFinite(temperature, "temperature");
        validateFinite(humidity, "humidity");
        validateFinite(soilMoisture, "soilMoisture");
        validateFinite(lightLevel, "lightLevel");
        validateFinite(nutrientLevel, "nutrientLevel");
        validateNonNegative(waterLitres, "waterLitres");
        validateNonNegative(energyKwh, "energyKwh");
        validateNonNegative(nutrientsUsedPercent, "nutrientsUsedPercent");
    }

    private static void validateFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void validateNonNegative(double value, String name) {
        validateFinite(value, name);
        if (value < 0) {
            throw new IllegalArgumentException(name + " cannot be negative");
        }
    }
}
