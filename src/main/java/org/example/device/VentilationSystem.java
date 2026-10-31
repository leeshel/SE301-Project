package org.device;

import org.example.SVFRMS.ChamberState;

public class VentilationSystem implements AutomatedDevice {

    @Override
    public String getDeviceName() {
        return "Ventilation System";
    }

    @Override
    public String activate(ChamberState state) {
        state.humidity = Math.max(30, state.humidity - 5);

        return "Humidity reduced to " + state.humidity + "%";
    }
}