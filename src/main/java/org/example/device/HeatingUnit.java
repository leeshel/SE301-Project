package org.example.device;

import org.example.sensor.ChamberState;
import org.example.sensor.SensorType;

public class HeatingUnit implements AutomatedDevice {

    @Override
    public String getDeviceName() {
        return "Heating Unit";
    }

    @Override
    public String activate(ChamberState state) {
        double temperature = Math.min(35, state.getValue(SensorType.TEMPERATURE) + 5);
        state.setValue(SensorType.TEMPERATURE, temperature);

        return "Temperature increased to " + temperature + "C";
    }
}
