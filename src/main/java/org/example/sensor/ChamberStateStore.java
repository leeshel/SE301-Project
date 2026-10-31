package org.example.sensor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChamberStateStore {
    private final Map<String, ChamberState> states = new ConcurrentHashMap<>();

    public ChamberState getOrCreateState(String chamber) {
        ChamberState state = states.get(chamber);
        if (state != null) {
            return state;
        }
        // putIfAbsent checks and inserts as one step, so two threads cannot both insert.
        ChamberState created = new ChamberState();
        ChamberState existing = states.putIfAbsent(chamber, created);
        return existing == null ? created : existing;
    }
}
