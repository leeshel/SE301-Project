package org.example.device;

import org.example.sensor.ChamberState;

public interface AutomatedDevice {

    String getDeviceName();

    String activate(ChamberState state);
}
