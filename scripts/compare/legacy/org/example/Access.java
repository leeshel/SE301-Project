package org.example;

import java.util.ArrayList;
import java.util.List;

class Access {
    static void reset() {
        SVFRMS.alerts.clear();
        SVFRMS.sensorLogs.clear();
        SVFRMS.controlQueue.clear();
        SVFRMS.chamberStates.clear();
        SVFRMS.chamberSummaries.clear();
        SVFRMS.totalWaterUsage = 0;
        SVFRMS.totalEnergyUsage = 0;
        SVFRMS.totalAlerts = 0;
        SVFRMS.totalAlertsGenerated = 0;
    }

    static void process(String chamber, String crop, String sensor, double value) {
        SVFRMS.processSensor(chamber, crop, sensor, value);
    }

    static void activate(String chamber, String control) {
        SVFRMS.activateControl(chamber, control);
    }

    static void set(String chamber, double t, double h, double soil, double light, double n) {
        SVFRMS.ChamberState s = SVFRMS.getChamberState(chamber);
        s.temperature = t;
        s.humidity = h;
        s.soilMoisture = soil;
        s.lightLevel = light;
        s.nutrientLevel = n;
    }

    static String lastAction(String chamber) {
        return SVFRMS.getChamberSummary(chamber).lastAction;
    }

    static String snapshot(String chamber) {
        SVFRMS.ChamberState s = SVFRMS.getChamberState(chamber);
        SVFRMS.ChamberSummary c = SVFRMS.getChamberSummary(chamber);
        List<String> logs = new ArrayList<>();
        for (String log : SVFRMS.sensorLogs) {
            logs.add(log.substring(log.indexOf(" : ") + 3));
        }
        return "alerts=" + SVFRMS.alerts
                + " queue=" + SVFRMS.controlQueue
                + " water=" + SVFRMS.totalWaterUsage
                + " energy=" + SVFRMS.totalEnergyUsage
                + " active=" + SVFRMS.totalAlerts
                + " generated=" + SVFRMS.totalAlertsGenerated
                + " state=" + s.temperature + "/" + s.humidity + "/" + s.soilMoisture
                + "/" + s.lightLevel + "/" + s.nutrientLevel
                + " summary=" + c.alerts + "/" + c.mitigationActions + "/" + c.lastAlert
                + " logs=" + logs;
    }
}
