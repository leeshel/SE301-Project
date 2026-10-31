package org.example.sensor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChamberStateStore {
    private final Map<String, ChamberState> states = new ConcurrentHashMap<>();

    public ChamberState getState(String chamber) {
        return states.computeIfAbsent(chamber, key -> new ChamberState());
    }
}
