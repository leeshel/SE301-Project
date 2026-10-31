package org.device;

import org.example.SVFRMS.ChamberState;

public class WaterPump implements AutomatedDevice {

    @Override
    public String getDeviceName() {
        return "Water Pump";
    }

    @Override
    public String activate(ChamberState state) {
        state.soilMoisture = Math.min(100, state.soilMoisture + 20);

        return "Soil moisture increased to " + state.soilMoisture + "%";
    }
}