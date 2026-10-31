package org.example.device;

import org.example.SVFRMS.ChamberState;

public class NutrientInjector implements AutomatedDevice {

    @Override
    public String getDeviceName() {
        return "Nutrient Injector";
    }

    @Override
    public String activate(ChamberState state) {
        state.nutrientLevel = Math.min(100, state.nutrientLevel + 15);

        return "Nutrient level increased to " + state.nutrientLevel + "%";
    }
}