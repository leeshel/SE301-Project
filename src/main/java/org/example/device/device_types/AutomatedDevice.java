package org.example.device.device_types;

import org.example.sensor.ChamberState;

public interface AutomatedDevice {

    String getDeviceName();

    String activate(ChamberState state);
}
