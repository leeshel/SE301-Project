package org.example.crop;

public final class DefaultCropProfiles {
    // Legacy applies a 500 lux minimum to every crop and caps Grow Light output at 1000 lux.
    private static final double MIN_LIGHT_LUX = 500;
    private static final double MAX_LIGHT_LUX = 1000;

    private DefaultCropProfiles() {
    }

    // Loads the six existing crops
    public static void loadInto(CropRepository repository) {
        repository.save(new CropBuilder("Tomato")
                .temperatureRange(22, 28)
                .humidityRange(60, 80)
                .soilMoistureRange(45, 60)
                .lightLevelRange(MIN_LIGHT_LUX, MAX_LIGHT_LUX)
                .build());
        repository.save(new CropBuilder("Strawberry")
                .temperatureRange(18, 24)
                .humidityRange(70, 85)
                .soilMoistureRange(50, 65)
                .lightLevelRange(MIN_LIGHT_LUX, MAX_LIGHT_LUX)
                .build());
        repository.save(new CropBuilder("Lettuce")
                .temperatureRange(15, 21)
                .humidityRange(65, 85)
                .soilMoistureRange(55, 75)
                .lightLevelRange(MIN_LIGHT_LUX, MAX_LIGHT_LUX)
                .build());
        repository.save(new CropBuilder("Spinach")
                .temperatureRange(10, 20)
                .lightLevelRange(MIN_LIGHT_LUX, MAX_LIGHT_LUX)
                .build());
        repository.save(new CropBuilder("Chili")
                .temperatureRange(24, 32)
                .lightLevelRange(MIN_LIGHT_LUX, MAX_LIGHT_LUX)
                .build());
        repository.save(new CropBuilder("Cucumber")
                .temperatureRange(18, 27)
                .lightLevelRange(MIN_LIGHT_LUX, MAX_LIGHT_LUX)
                .build());
    }
}
