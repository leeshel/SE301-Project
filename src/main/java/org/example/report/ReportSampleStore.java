package org.example.report;

import java.util.ArrayList;
import java.util.List;

public class ReportSampleStore {
    private final List<ReportSample> samples = new ArrayList<>();

    public void add(ReportSample sample) {
        synchronized (samples) {
            samples.add(sample);
        }
    }

    public List<ReportSample> getAll() {
        synchronized (samples) {
            return List.copyOf(samples);
        }
    }
}
