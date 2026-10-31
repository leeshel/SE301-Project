package org.device;

import org.example.SVFRMS.ChamberState;

public class HeatingUnit implements AutomatedDevice {

    @Override
    public String getDeviceName() {
        return "Heating Unit";
    }

    @Override
    public String activate(ChamberState state) {
        state.temperature = Math.min(35, state.temperature + 5);

        return "Temperature increased to " + state.temperature + "C";
    }
}