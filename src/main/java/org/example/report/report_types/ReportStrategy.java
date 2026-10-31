package org.example.report.report_types;

import org.example.report.ReportSample;
import org.example.report.ReportType;

import java.util.List;

public interface ReportStrategy {
    ReportType getType();

    List<String> buildLines(List<ReportSample> samples);
}
