package org.example.report;

import java.util.List;

public interface ReportStrategy {
    ReportType getType();

    List<String> buildLines(List<ReportSample> samples);
}
