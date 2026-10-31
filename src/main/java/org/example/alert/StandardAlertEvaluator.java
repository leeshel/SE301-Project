package org.example.alert;

import org.example.crop.CropProfile;
import org.example.crop.Range;
import org.example.sensor.SensorType;

import java.util.ArrayList;
import java.util.List;

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
        addRangeAlert(alerts, observation, profile.getTemperatureRange().orElse(null),
                SensorType.TEMPERATURE, AlertType.TEMPERATURE, "Temperature");
        addRangeAlert(alerts, observation, profile.getHumidityRange().orElse(null),
                SensorType.HUMIDITY, AlertType.HUMIDITY, "Humidity");
        addLowAlert(alerts, observation, profile.getSoilMoistureRange().orElse(null),
                AlertType.LOW_SOIL_MOISTURE, "Soil moisture");
        addNutrientAlert(alerts, observation);
        addSensorFailureAlerts(alerts, observation);
        return List.copyOf(alerts);
    }

    private void addRangeAlert(List<Alert> alerts, EnvironmentalObservation observation,
                               Range range, SensorType sensorType, AlertType alertType,
                               String label) {
        if (range == null || !observation.hasValue(sensorType)) {
            return;
        }
        double value = observation.valueOf(sensorType);
        if (!Double.isFinite(value) || range.min() <= value && value <= range.max()) {
            return;
        }
        double distance = value < range.min() ? range.min() - value : value - range.max();
        double span = Math.max(range.max() - range.min(), 1);
        AlertSeverity severity = distance >= span * 0.25
                ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
        alerts.add(alert(observation, alertType, severity,
                label + " is outside the acceptable range (" + value + ")"));
    }

    private void addLowAlert(List<Alert> alerts, EnvironmentalObservation observation,
                             Range range, AlertType type, String label) {
        if (range == null || !observation.hasValue(SensorType.SOIL_MOISTURE)) {
            return;
        }
        double value = observation.valueOf(SensorType.SOIL_MOISTURE);
        if (Double.isFinite(value) && value >= range.min()) {
            return;
        }
        AlertSeverity severity = Double.isFinite(value) && value < range.min() * 0.8
                ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
        alerts.add(alert(observation, type, severity,
                label + " is below the minimum required level (" + value + ")"));
    }

    private void addNutrientAlert(List<Alert> alerts, EnvironmentalObservation observation) {
        if (!observation.hasValue(SensorType.NUTRIENT_LEVEL)) {
            return;
        }
        double value = observation.valueOf(SensorType.NUTRIENT_LEVEL);
        if (Double.isFinite(value) && value >= nutrientMinimum) {
            return;
        }
        AlertSeverity severity = Double.isFinite(value) && value < nutrientMinimum * 0.5
                ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
        alerts.add(alert(observation, AlertType.NUTRIENT_DEFICIENCY, severity,
                "Nutrient level is below the minimum required level (" + value + ")"));
    }

    private void addSensorFailureAlerts(List<Alert> alerts, EnvironmentalObservation observation) {
        for (SensorType type : SensorType.values()) {
            if (observation.hasInvalidValue(type)) {
                alerts.add(alert(observation, AlertType.SENSOR_FAILURE, AlertSeverity.CRITICAL,
                        type.getLabel() + " sensor is missing or reported an invalid reading"));
            }
        }
    }

    private Alert alert(EnvironmentalObservation observation, AlertType type,
                        AlertSeverity severity, String message) {
        return new Alert(observation.chamber(), observation.crop(), type, severity,
                message, observation.observedAt());
    }
}
