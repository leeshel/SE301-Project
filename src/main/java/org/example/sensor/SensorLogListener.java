package org.example.sensor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SensorLogListener implements ReadingListener {
    private final List<String> logs = new ArrayList<>();

    @Override
    public void onReading(SensorReading reading) {
        String entry = new Date() + " : Processing " + reading.crop() + " "
                + reading.type().getLabel() + " = " + reading.value();
        synchronized (logs) {
            logs.add(entry);
        }
    }

    public List<String> getLogs() {
        synchronized (logs) {
            return List.copyOf(logs);
        }
    }
}
