package org.example.device;

import org.example.SVFRMS.ChamberState;

public class Dehumidifier implements AutomatedDevice {

    @Override
    public String getDeviceName() {
        return "Dehumidifier";
    }

    @Override
    public String activate(ChamberState state) {
        state.humidity = Math.max(30, state.humidity - 10);

        return "Humidity reduced to " + state.humidity + "%";
    }
}