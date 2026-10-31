package org.example.usage;

import org.example.alert.Alert;
import org.example.alert.AlertHandler;

import java.util.HashMap;
import java.util.Map;

public class UsageTracker implements AlertHandler {
    private static final int LOW_SOIL_WATER_LITRES = 50;
    private static final int CYCLE_WATER_LITRES = 2;
    private static final int CYCLE_ENERGY_KWH = 1;
    private static final int IRRIGATION_WATER_LITRES = 100;
    private static final int FEEDING_ENERGY_KWH = 20;
    // Matches the amount NutrientInjector adds per activation.
    private static final int INJECTOR_NUTRIENT_PERCENT = 15;

    private final Object lock = new Object();
    private final Map<String, CropUsageRates> rates;
    private final Map<String, ChamberUsage> pending = new HashMap<>();
    private int totalWater = 0;
    private int totalEnergy = 0;

    public UsageTracker(Map<String, CropUsageRates> rates) {
        this.rates = Map.copyOf(rates);
    }

    @Override
    public void onAlert(Alert alert) {
        CropUsageRates crop = rates.getOrDefault(alert.crop(), CropUsageRates.NONE);
        switch (alert.type()) {
            case TEMPERATURE -> {
                double energy = alert.message().equals("High Temp")
                        ? crop.highTempEnergyKwh() : crop.lowTempEnergyKwh();
                addUsage(alert.chamber(), 0, energy, 0);
            }
            case HUMIDITY -> {
                if (alert.message().equals("Low Humidity")) {
                    addUsage(alert.chamber(), crop.lowHumidityWaterLitres(), 0, 0);
                }
            }
            case LOW_SOIL_MOISTURE -> addUsage(alert.chamber(), LOW_SOIL_WATER_LITRES, 0, 0);
            case NUTRIENT_DEFICIENCY -> addUsage(alert.chamber(), 0, 0, INJECTOR_NUTRIENT_PERCENT);
            default -> {
            }
        }
    }

    public void recordCycle(String chamber) {
        addUsage(chamber, CYCLE_WATER_LITRES, CYCLE_ENERGY_KWH, 0);
    }

    public void recordIrrigationCycle() {
        synchronized (lock) {
            totalWater += IRRIGATION_WATER_LITRES;
        }
    }

    public void recordFeedingCycle() {
        synchronized (lock) {
            totalEnergy += FEEDING_ENERGY_KWH;
        }
    }

    // Returns the usage recorded for this chamber since the last call, then resets it.
    public ChamberUsage takeChamberUsage(String chamber) {
        synchronized (lock) {
            ChamberUsage usage = pending.remove(chamber);
            return usage == null ? new ChamberUsage(0, 0, 0) : usage;
        }
    }

    public int getTotalWater() {
        synchronized (lock) {
            return totalWater;
        }
    }

    public int getTotalEnergy() {
        synchronized (lock) {
            return totalEnergy;
        }
    }

    private void addUsage(String chamber, double water, double energy, double nutrients) {
        synchronized (lock) {
            totalWater += (int) water;
            totalEnergy += (int) energy;
            ChamberUsage current = pending.get(chamber);
            if (current == null) {
                current = new ChamberUsage(0, 0, 0);
            }
            pending.put(chamber, new ChamberUsage(current.waterLitres() + water,
                    current.energyKwh() + energy, current.nutrientsPercent() + nutrients));
        }
    }
}
