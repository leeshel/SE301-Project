package org.example.sensor;

import java.util.List;

public class ChamberMonitor implements Runnable {
    private final String chamber;
    private final String crop;
    private final List<Sensor> sensors;
    private final List<ReadingListener> listeners;
    private final int cycles;

    public ChamberMonitor(String chamber, String crop, List<Sensor> sensors,
                          List<ReadingListener> listeners, int cycles) {
        this.chamber = chamber;
        this.crop = crop;
        this.sensors = List.copyOf(sensors);
        this.listeners = List.copyOf(listeners);
        this.cycles = cycles;
    }

    @Override
    public void run() {
        for (int i = 0; i < cycles; i++) {
            for (Sensor sensor : sensors) {
                publish(new SensorReading(chamber, crop, sensor.getType(), sensor.read()));
            }
        }
    }

    private void publish(SensorReading reading) {
        for (ReadingListener listener : listeners) {
            listener.onReading(reading);
        }
    }
}
