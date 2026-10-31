package org.example.report;

import org.example.alert.Alert;
import org.example.alert.AlertHandler;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class ChamberSummaryTracker implements AlertHandler {
    private final Map<String, ChamberSummary> summaries = new ConcurrentHashMap<>();

    @Override
    public void onAlert(Alert alert) {
        getOrCreateSummary(alert.chamber()).recordAlert(alert.displayText());
    }

    public void recordAction(String chamber, String action) {
        getOrCreateSummary(chamber).recordAction(action);
    }

    public ChamberSummary getOrCreateSummary(String chamber) {
        ChamberSummary summary = summaries.get(chamber);
        if (summary != null) {
            return summary;
        }
        // putIfAbsent checks and inserts as one step, so two threads cannot both insert.
        ChamberSummary created = new ChamberSummary();
        ChamberSummary existing = summaries.putIfAbsent(chamber, created);
        return existing == null ? created : existing;
    }

    // Sorted by chamber name.
    public Map<String, ChamberSummary> getAll() {
        return new TreeMap<>(summaries);
    }
}
