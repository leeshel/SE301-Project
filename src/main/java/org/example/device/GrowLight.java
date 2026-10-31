package org.example.device;

import org.example.SVFRMS.ChamberState;

public class GrowLight implements AutomatedDevice {

    @Override
    public String getDeviceName() {
        return "Grow Light";
    }

    @Override
    public String activate(ChamberState state) {
        state.lightLevel = Math.min(1000, state.lightLevel + 150);

        return "Light level increased to " + state.lightLevel + " lux";
    }
}