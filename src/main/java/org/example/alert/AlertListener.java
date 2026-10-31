package org.example.alert;

import org.example.crop.CropProfile;
import org.example.crop.CropRepository;
import org.example.sensor.ReadingListener;
import org.example.sensor.SensorReading;

import java.util.List;

public class AlertListener implements ReadingListener {
    private final CropRepository crops;
    private final ReadingAlertEvaluator evaluator;
    private final List<AlertHandler> handlers;

    public AlertListener(CropRepository crops, ReadingAlertEvaluator evaluator,
                         List<AlertHandler> handlers) {
        this.crops = crops;
        this.evaluator = evaluator;
        this.handlers = List.copyOf(handlers);
    }

    @Override
    public void onReading(SensorReading reading) {
        CropProfile profile = crops.findByName(reading.crop()).orElse(null);
        for (Alert alert : evaluator.evaluate(reading, profile)) {
            for (AlertHandler handler : handlers) {
                handler.onAlert(alert);
            }
        }
    }
}
