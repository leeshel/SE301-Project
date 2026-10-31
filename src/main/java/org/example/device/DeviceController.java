package org.example.device;

import java.util.HashMap;
import java.util.Map;

import org.example.SVFRMS.ChamberState;

public class DeviceController {

    private final Map<String, AutomatedDevice> devices;

    public DeviceController() {
        devices = new HashMap<>();

        registerDevice(new CoolingFan());
        registerDevice(new WaterPump());
        registerDevice(new NutrientInjector());
        registerDevice(new Humidifier());
        registerDevice(new Dehumidifier());
        registerDevice(new GrowLight());
        registerDevice(new HeatingUnit());
        registerDevice(new VentilationSystem());
        registerDevice(new ShadeController());
    }

    // Register a device using its name as the key
    public void registerDevice(AutomatedDevice device) {
        devices.put(device.getDeviceName(), device);
    }

    // Activate the requested device
    public String activateDevice(String deviceName, ChamberState state) {

        AutomatedDevice device = devices.get(deviceName);

        if (device == null) {
            throw new IllegalArgumentException(
                "Unknown device: " + deviceName
            );
        }

        return device.activate(state);
    }

    // Check whether a device is registered
    public boolean hasDevice(String deviceName) {
        return devices.containsKey(deviceName);
    }
}