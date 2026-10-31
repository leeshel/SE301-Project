package org.example.alert;

public class AlertPrinter implements AlertHandler {
    @Override
    public void onAlert(Alert alert) {
        System.out.println("[" + alert.chamber() + "][" + alert.crop() + "][ALERT] "
                + alert.displayText());
    }
}
