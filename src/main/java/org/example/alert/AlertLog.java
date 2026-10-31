package org.example.alert;

import java.util.ArrayList;
import java.util.List;

public class AlertLog implements AlertHandler {
    private final Object lock = new Object();
    private final List<String> active = new ArrayList<>();
    private final List<String> archived = new ArrayList<>();
    private int totalGenerated = 0;

    @Override
    public void onAlert(Alert alert) {
        String text = alert.displayText();
        synchronized (lock) {
            active.add(text);
            totalGenerated++;
        }
    }

    // Moves every active alert to the archive and returns how many were moved.
    public int archiveActive() {
        synchronized (lock) {
            int moved = active.size();
            archived.addAll(active);
            active.clear();
            return moved;
        }
    }

    public int getActiveCount() {
        synchronized (lock) {
            return active.size();
        }
    }

    public int getTotalGenerated() {
        synchronized (lock) {
            return totalGenerated;
        }
    }

    public List<String> getActive() {
        synchronized (lock) {
            return List.copyOf(active);
        }
    }

    public List<String> getArchived() {
        synchronized (lock) {
            return List.copyOf(archived);
        }
    }
}
