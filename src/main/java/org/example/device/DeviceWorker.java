package org.example.device;

import org.example.report.ChamberSummaryTracker;
import org.example.sensor.ChamberStateStore;

public class DeviceWorker implements Runnable {
    private final ControlQueue queue;
    private final DeviceController controller;
    private final ChamberStateStore states;
    private final ChamberSummaryTracker summaries;

    public DeviceWorker(ControlQueue queue, DeviceController controller,
                        ChamberStateStore states, ChamberSummaryTracker summaries) {
        this.queue = queue;
        this.controller = controller;
        this.states = states;
        this.summaries = summaries;
    }

    @Override
    public void run() {
        ControlRequest request = queue.take();
        while (request != null) {
            // If this thread died, chambers would wait forever on a full queue.
            try {
                activateRequestedDevice(request);
            } catch (RuntimeException e) {
                System.out.println("Could not activate " + request.device() + ": " + e.getMessage());
            }
            request = queue.take();
        }
    }

    public void activateRequestedDevice(ControlRequest request) {
        if (!controller.hasDevice(request.device())) {
            return;
        }
        String chamber = request.chamber();
        String result = controller.activateDevice(request.device(), states.getOrCreateState(chamber));
        String action = request.device() + " Activated";
        summaries.recordAction(chamber, action);
        printControlMessage(chamber, action);
        printControlMessage(chamber, result);
    }

    private void printControlMessage(String chamber, String message) {
        System.out.println("\t\u001B[32m[" + chamber + "][CONTROL] " + message + "\u001B[0m");
    }
}
