package org.example.device;

import org.example.sensor.ChamberState;
import org.example.sensor.SensorType;

public class WaterPump implements AutomatedDevice {

    @Override
    public String getDeviceName() {
        return "Water Pump";
    }

    @Override
    public String activate(ChamberState state) {
        double soilMoisture = Math.min(100, state.getValue(SensorType.SOIL_MOISTURE) + 20);
        state.setValue(SensorType.SOIL_MOISTURE, soilMoisture);

        return "Soil moisture increased to " + soilMoisture + "%";
    }
}
