package org.example.device.device_types;

import org.example.sensor.ChamberState;
import org.example.sensor.SensorType;

public class VentilationSystem implements AutomatedDevice {

    @Override
    public String getDeviceName() {
        return "Ventilation System";
    }

    @Override
    public String activate(ChamberState state) {
        double humidity = Math.max(30, state.getValue(SensorType.HUMIDITY) - 5);
        state.setValue(SensorType.HUMIDITY, humidity);

        return "Humidity reduced to " + humidity + "%";
    }
}
