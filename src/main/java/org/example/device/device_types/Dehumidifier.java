package org.example.device.device_types;

import org.example.sensor.ChamberState;
import org.example.sensor.SensorType;

public class Dehumidifier implements AutomatedDevice {

    @Override
    public String getDeviceName() {
        return "Dehumidifier";
    }

    @Override
    public String activate(ChamberState state) {
        double humidity = Math.max(30, state.getValue(SensorType.HUMIDITY) - 10);
        state.setValue(SensorType.HUMIDITY, humidity);

        return "Humidity reduced to " + humidity + "%";
    }
}
