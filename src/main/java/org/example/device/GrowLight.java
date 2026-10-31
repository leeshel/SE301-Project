package org.example.device;

import org.example.sensor.ChamberState;
import org.example.sensor.SensorType;

public class GrowLight implements AutomatedDevice {

    @Override
    public String getDeviceName() {
        return "Grow Light";
    }

    @Override
    public String activate(ChamberState state) {
        double lightLevel = Math.min(1000, state.getValue(SensorType.LIGHT_LEVEL) + 150);
        state.setValue(SensorType.LIGHT_LEVEL, lightLevel);

        return "Light level increased to " + lightLevel + " lux";
    }
}
