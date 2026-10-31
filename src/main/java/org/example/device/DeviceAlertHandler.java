package org.example.device;

import org.example.alert.Alert;
import org.example.alert.AlertHandler;
import org.example.alert.AlertType;

import java.util.Map;

public class DeviceAlertHandler implements AlertHandler {
    // Low temperature requests no device because legacy never activated the heater.
    private static final Map<AlertType, String> DEVICES = Map.of(
            AlertType.HIGH_TEMPERATURE, "Cooling Fan",
            AlertType.HIGH_HUMIDITY, "Dehumidifier",
            AlertType.LOW_HUMIDITY, "Humidifier",
            AlertType.LOW_SOIL_MOISTURE, "Water Pump",
            AlertType.LOW_LIGHT, "Grow Light",
            AlertType.NUTRIENT_DEFICIENCY, "Nutrient Injector");

    private final ControlQueue queue;

    public DeviceAlertHandler(ControlQueue queue) {
        this.queue = queue;
    }

    @Override
    public void onAlert(Alert alert) {
        String device = DEVICES.get(alert.type());
        if (device != null) {
            queue.put(new ControlRequest(alert.chamber(), device));
        }
    }
}
