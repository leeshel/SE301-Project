package org.example.device;

import org.example.sensor.ChamberState;
import org.example.sensor.SensorType;

public class ShadeController implements AutomatedDevice {

    @Override
    public String getDeviceName() {
        return "Shade Controller";
    }

    @Override
    public String activate(ChamberState state) {
        double lightLevel = Math.max(0, state.getValue(SensorType.LIGHT_LEVEL) - 150);
        state.setValue(SensorType.LIGHT_LEVEL, lightLevel);

        return "Light level reduced to " + lightLevel + " lux";
    }
}
