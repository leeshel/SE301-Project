package org.example.usage;

import java.util.Map;

public final class DefaultUsageRates {
    private DefaultUsageRates() {
    }

    // Legacy only charged extra usage for these two crops; every other crop uses NONE.
    public static Map<String, CropUsageRates> create() {
        return Map.of(
                "Tomato", new CropUsageRates(10, 15, 20),
                "Strawberry", new CropUsageRates(10, 15, 25));
    }
}
