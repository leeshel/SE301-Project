package org.example.alert;

import org.example.crop.Range;
import org.example.sensor.SensorReading;

import java.time.Instant;
import java.util.Optional;

// Logic shared by the rules that compare a reading with a range or a minimum.
final class AlertChecks {
    private AlertChecks() {
    }

    static Optional<Alert> checkRange(SensorReading reading, Range range,
                                      AlertType highType, String highMessage,
                                      AlertType lowType, String lowMessage) {
        double value = reading.value();
        if (range == null || !Double.isFinite(value)
                || range.min() <= value && value <= range.max()) {
            return Optional.empty();
        }
        boolean low = value < range.min();
        double distance = low ? range.min() - value : value - range.max();
        double span = Math.max(range.max() - range.min(), 1);
        AlertSeverity severity = distance >= span * 0.25
                ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
        return Optional.of(createAlert(reading, low ? lowType : highType, severity,
                low ? lowMessage : highMessage));
    }

    static Optional<Alert> checkFixedMinimum(SensorReading reading, double minimum,
                                             AlertType type, String message) {
        if (reading.value() < minimum) {
            return Optional.of(createAlert(reading, type, AlertSeverity.WARNING, message));
        }
        return Optional.empty();
    }

    static Alert createAlert(SensorReading reading, AlertType type,
                             AlertSeverity severity, String message) {
        return new Alert(reading.chamber(), reading.crop(), type, severity, message, Instant.now());
    }
}
