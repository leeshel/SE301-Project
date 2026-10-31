package org.example.device;

import org.example.SVFRMS.ChamberState;

public class CoolingFan implements AutomatedDevice {

    @Override
    public String getDeviceName() {
        return "Cooling Fan";
    }

    @Override
    public String activate(ChamberState state) {
        state.temperature = Math.max(18, state.temperature - 5);

        return "Temperature reduced to " + state.temperature + "C";
    }
}