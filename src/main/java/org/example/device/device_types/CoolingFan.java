package org.example.device.device_types;

import org.example.sensor.ChamberState;
import org.example.sensor.SensorType;

public class CoolingFan implements AutomatedDevice {

    @Override
    public String getDeviceName() {
        return "Cooling Fan";
    }

    @Override
    public String activate(ChamberState state) {
        double temperature = Math.max(18, state.getValue(SensorType.TEMPERATURE) - 5);
        state.setValue(SensorType.TEMPERATURE, temperature);

        return "Temperature reduced to " + temperature + "C";
    }
}
