package org.example.alert.alert_rules;

import java.util.List;

public final class DefaultAlertRules {
    private DefaultAlertRules() {
    }

    public static List<AlertRule> create() {
        return List.of(
                new TemperatureRule(),
                new HumidityRule(),
                new LowSoilMoistureRule(),
                new LowLightRule(),
                new NutrientDeficiencyRule(),
                new SensorFailureRule());
    }
}
