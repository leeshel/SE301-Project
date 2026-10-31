package org.example.device;

import org.example.sensor.ChamberState;
import org.example.sensor.SensorType;

public class Humidifier implements AutomatedDevice {

    @Override
    public String getDeviceName() {
        return "Humidifier";
    }

    @Override
    public String activate(ChamberState state) {
        double humidity = Math.min(100, state.getValue(SensorType.HUMIDITY) + 10);
        state.setValue(SensorType.HUMIDITY, humidity);

        return "Humidity increased to " + humidity + "%";
    }
}
