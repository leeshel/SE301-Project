package org.example.report;

public class ChamberSummary {
    private final Object lock = new Object();
    private int alerts = 0;
    private int mitigationActions = 0;
    private String lastAction = "No action";
    private String lastAlert = "None";

    public void recordAlert(String alert) {
        synchronized (lock) {
            alerts++;
            lastAlert = alert;
        }
    }

    public void recordAction(String action) {
        synchronized (lock) {
            mitigationActions++;
            lastAction = action;
        }
    }

    public int getAlerts() {
        synchronized (lock) {
            return alerts;
        }
    }

    public int getMitigationActions() {
        synchronized (lock) {
            return mitigationActions;
        }
    }

    public String getLastAction() {
        synchronized (lock) {
            return lastAction;
        }
    }

    public String getLastAlert() {
        synchronized (lock) {
            return lastAlert;
        }
    }
}
