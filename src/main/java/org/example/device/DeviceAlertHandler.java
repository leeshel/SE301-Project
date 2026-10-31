package org.example.device;

import org.example.alert.Alert;
import org.example.alert.AlertHandler;

public class DeviceAlertHandler implements AlertHandler {
    private final ControlQueue queue;

    public DeviceAlertHandler(ControlQueue queue) {
        this.queue = queue;
    }

    @Override
    public void onAlert(Alert alert) {
        String device = deviceFor(alert);
        if (device != null) {
            queue.put(new ControlRequest(alert.chamber(), device));
        }
    }

    // Low temperature requests no device because legacy never activated the heater.
    private String deviceFor(Alert alert) {
        return switch (alert.type()) {
            case TEMPERATURE -> alert.message().equals("High Temp") ? "Cooling Fan" : null;
            case HUMIDITY -> alert.message().equals("High Humidity") ? "Dehumidifier" : "Humidifier";
            case LOW_SOIL_MOISTURE -> "Water Pump";
            case LOW_LIGHT -> "Grow Light";
            case NUTRIENT_DEFICIENCY -> "Nutrient Injector";
            case SENSOR_FAILURE -> null;
        };
    }
}
