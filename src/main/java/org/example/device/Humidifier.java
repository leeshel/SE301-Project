package org.device;

import org.example.SVFRMS.ChamberState;

public class Humidifier implements AutomatedDevice {

    @Override
    public String getDeviceName() {
        return "Humidifier";
    }

    @Override
    public String activate(ChamberState state) {
        state.humidity = Math.min(100, state.humidity + 10);

        return "Humidity increased to " + state.humidity + "%";
    }
}