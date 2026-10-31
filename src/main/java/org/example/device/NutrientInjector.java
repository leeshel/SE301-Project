package org.example.device;

import org.example.sensor.ChamberState;
import org.example.sensor.SensorType;

public class NutrientInjector implements AutomatedDevice {

    @Override
    public String getDeviceName() {
        return "Nutrient Injector";
    }

    @Override
    public String activate(ChamberState state) {
        double nutrientLevel = Math.min(100, state.getValue(SensorType.NUTRIENT_LEVEL) + 15);
        state.setValue(SensorType.NUTRIENT_LEVEL, nutrientLevel);

        return "Nutrient level increased to " + nutrientLevel + "%";
    }
}
