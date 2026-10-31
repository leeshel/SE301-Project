package org.example.alert;

import org.example.alert.alert_rules.AlertRule;
import org.example.crop.CropProfile;
import org.example.crop.CropRepository;
import org.example.sensor.ReadingListener;
import org.example.sensor.SensorReading;

import java.util.List;
import java.util.Optional;

public class AlertListener implements ReadingListener {
    private final CropRepository crops;
    private final List<AlertRule> rules;
    private final List<AlertHandler> handlers;

    public AlertListener(CropRepository crops, List<AlertRule> rules, List<AlertHandler> handlers) {
        this.crops = crops;
        this.rules = List.copyOf(rules);
        this.handlers = List.copyOf(handlers);
    }

    @Override
    public void onReading(SensorReading reading) {
        CropProfile profile = crops.findByName(reading.crop()).orElse(null);
        for (AlertRule rule : rules) {
            Optional<Alert> alert = rule.check(reading, profile);
            if (alert.isPresent()) {
                notifyHandlers(alert.get());
            }
        }
    }

    private void notifyHandlers(Alert alert) {
        for (AlertHandler handler : handlers) {
            handler.onAlert(alert);
        }
    }
}
