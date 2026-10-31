package org.device;

import org.example.SVFRMS.ChamberState;

public class ShadeController implements AutomatedDevice {

    @Override
    public String getDeviceName() {
        return "Shade Controller";
    }

    @Override
    public String activate(ChamberState state) {
        state.lightLevel = Math.max(0, state.lightLevel - 150);

        return "Light level reduced to " + state.lightLevel + " lux";
    }
}