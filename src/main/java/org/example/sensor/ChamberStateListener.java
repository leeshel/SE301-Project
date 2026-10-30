package org.example.sensor;

public class ChamberStateListener implements ReadingListener {
    private final ChamberStateStore store;

    public ChamberStateListener(ChamberStateStore store) {
        this.store = store;
    }

    @Override
    public void onReading(SensorReading reading) {
        store.getState(reading.chamber()).setValue(reading.type(), reading.value());
    }
}
