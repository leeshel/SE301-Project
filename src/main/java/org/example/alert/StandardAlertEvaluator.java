package org.example.alert;

import org.example.crop.CropProfile;
import org.example.crop.Range;
import org.example.sensor.SensorType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StandardAlertEvaluator implements AlertEvaluator {
    private final double nutrientMinimum;

    public StandardAlertEvaluator(double nutrientMinimum) {
        if (!Double.isFinite(nutrientMinimum) || nutrientMinimum < 0) {
            throw new IllegalArgumentException("Nutrient minimum must be non-negative and finite");
        }
        this.nutrientMinimum = nutrientMinimum;
    }

    public StandardAlertEvaluator() {
        this(50);
    }

    @Override
    public List<Alert> evaluate(EnvironmentalObservation observation, CropProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Crop profile is required");
        }

        List<Alert> alerts = new ArrayList<>();
        checkRange(observation, profile.getTemperatureRange().orElse(null),
                SensorType.TEMPERATURE, AlertType.TEMPERATURE, "Temperature").ifPresent(alerts::add);
        checkRange(observation, profile.getHumidityRange().orElse(null),
                SensorType.HUMIDITY, AlertType.HUMIDITY, "Humidity").ifPresent(alerts::add);
        checkLowSoilMoisture(observation, profile.getSoilMoistureRange().orElse(null))
                .ifPresent(alerts::add);
        checkNutrient(observation).ifPresent(alerts::add);
        alerts.addAll(findSensorFailures(observation));
        return List.copyOf(alerts);
    }

    private Optional<Alert> checkRange(EnvironmentalObservation observation, Range range,
                                       SensorType sensorType, AlertType alertType, String label) {
        if (range == null || !observation.hasValue(sensorType)) {
            return Optional.empty();
        }
        double value = observation.getValue(sensorType);
        if (!Double.isFinite(value) || range.min() <= value && value <= range.max()) {
            return Optional.empty();
        }
        double distance = value < range.min() ? range.min() - value : value - range.max();
        double span = Math.max(range.max() - range.min(), 1);
        AlertSeverity severity = distance >= span * 0.25
                ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
        return Optional.of(createAlert(observation, alertType, severity,
                label + " is outside the acceptable range (" + value + ")"));
    }

    private Optional<Alert> checkLowSoilMoisture(EnvironmentalObservation observation, Range range) {
        if (range == null || !observation.hasValue(SensorType.SOIL_MOISTURE)) {
            return Optional.empty();
        }
        double value = observation.getValue(SensorType.SOIL_MOISTURE);
        if (Double.isFinite(value) && value >= range.min()) {
            return Optional.empty();
        }
        AlertSeverity severity = Double.isFinite(value) && value < range.min() * 0.8
                ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
        return Optional.of(createAlert(observation, AlertType.LOW_SOIL_MOISTURE, severity,
                "Soil moisture is below the minimum required level (" + value + ")"));
    }

    private Optional<Alert> checkNutrient(EnvironmentalObservation observation) {
        if (!observation.hasValue(SensorType.NUTRIENT_LEVEL)) {
            return Optional.empty();
        }
        double value = observation.getValue(SensorType.NUTRIENT_LEVEL);
        if (Double.isFinite(value) && value >= nutrientMinimum) {
            return Optional.empty();
        }
        AlertSeverity severity = Double.isFinite(value) && value < nutrientMinimum * 0.5
                ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
        return Optional.of(createAlert(observation, AlertType.NUTRIENT_DEFICIENCY, severity,
                "Nutrient level is below the minimum required level (" + value + ")"));
    }

    private List<Alert> findSensorFailures(EnvironmentalObservation observation) {
        List<Alert> failures = new ArrayList<>();
        for (SensorType type : SensorType.values()) {
            if (observation.isMissingOrInvalid(type)) {
                failures.add(createAlert(observation, AlertType.SENSOR_FAILURE, AlertSeverity.CRITICAL,
                        type.getLabel() + " sensor is missing or reported an invalid reading"));
            }
        }
        return failures;
    }

    private Alert createAlert(EnvironmentalObservation observation, AlertType type,
                              AlertSeverity severity, String message) {
        return new Alert(observation.chamber(), observation.crop(), type, severity,
                message, observation.observedAt());
    }
}
