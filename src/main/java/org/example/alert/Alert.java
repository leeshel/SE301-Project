package org.example.alert;

import java.time.Instant;

public record Alert(
        String chamber,
        String crop,
        AlertType type,
        AlertSeverity severity,
        String message,
        Instant occurredAt) {

    public String displayText() {
        return switch (type) {
            case TEMPERATURE, HUMIDITY, LOW_SOIL_MOISTURE -> chamber + " " + crop + " " + message;
            case LOW_LIGHT, NUTRIENT_DEFICIENCY -> chamber + " " + message;
            case SENSOR_FAILURE -> chamber + " Sensor Failure";
        };
    }
}
