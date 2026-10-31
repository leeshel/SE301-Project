package org.example;

import org.example.device.ControlRequest;
import org.example.report.ChamberSummary;
import org.example.sensor.ChamberState;
import org.example.sensor.ReadingListener;
import org.example.sensor.SensorReading;
import org.example.sensor.SensorType;

import java.util.ArrayList;
import java.util.List;

class Access {
    private static FarmSystemFacade farm;

    static void reset() {
        farm = new FarmSystemFacade();
    }

    static void process(String chamber, String crop, String sensor, double value) {
        SensorReading reading = new SensorReading(chamber, crop, typeOf(sensor), value);
        for (ReadingListener listener : farm.createChamberListeners()) {
            listener.onReading(reading);
        }
    }

    static void activate(String chamber, String control) {
        farm.deviceWorker().activateRequestedDevice(new ControlRequest(chamber, control));
    }

    static void set(String chamber, double t, double h, double soil, double light, double n) {
        ChamberState s = farm.states().getOrCreateState(chamber);
        s.setValue(SensorType.TEMPERATURE, t);
        s.setValue(SensorType.HUMIDITY, h);
        s.setValue(SensorType.SOIL_MOISTURE, soil);
        s.setValue(SensorType.LIGHT_LEVEL, light);
        s.setValue(SensorType.NUTRIENT_LEVEL, n);
    }

    static String lastAction(String chamber) {
        return farm.summaries().getOrCreateSummary(chamber).getLastAction();
    }

    static String snapshot(String chamber) {
        ChamberState s = farm.states().getOrCreateState(chamber);
        ChamberSummary c = farm.summaries().getOrCreateSummary(chamber);
        List<String> logs = new ArrayList<>();
        for (String log : farm.sensorLog().getLogs()) {
            logs.add(log.substring(log.indexOf(" : ") + 3));
        }
        List<String> queue = new ArrayList<>();
        for (ControlRequest request : farm.controlQueue().getPendingRequests()) {
            queue.add(request.chamber() + "|" + request.device());
        }
        return "alerts=" + farm.alertLog().getActive()
                + " queue=" + queue
                + " water=" + farm.usage().getTotalWater()
                + " energy=" + farm.usage().getTotalEnergy()
                + " active=" + farm.alertLog().getActiveCount()
                + " generated=" + farm.alertLog().getTotalGenerated()
                + " state=" + s.getValue(SensorType.TEMPERATURE) + "/" + s.getValue(SensorType.HUMIDITY)
                + "/" + s.getValue(SensorType.SOIL_MOISTURE) + "/" + s.getValue(SensorType.LIGHT_LEVEL)
                + "/" + s.getValue(SensorType.NUTRIENT_LEVEL)
                + " summary=" + c.getAlerts() + "/" + c.getMitigationActions() + "/" + c.getLastAlert()
                + " logs=" + logs;
    }

    private static SensorType typeOf(String label) {
        for (SensorType type : SensorType.values()) {
            if (type.getLabel().equals(label)) {
                return type;
            }
        }
        throw new IllegalArgumentException(label);
    }
}
