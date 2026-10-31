package org.device;

import org.example.SVFRMS.ChamberState;

public interface AutomatedDevice {

    String getDeviceName();

    String activate(ChamberState state);
}
