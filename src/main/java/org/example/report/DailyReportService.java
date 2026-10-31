package org.example.report;

import org.example.alert.AlertLog;
import org.example.usage.UsageTracker;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DailyReportService {
    private static final Path ARCHIVE_DIR = Paths.get("archive");
    private static final Path REPORT_FILE = ARCHIVE_DIR.resolve("daily_report.txt");

    private final AlertLog alerts;
    private final UsageTracker usage;
    private final ChamberSummaryTracker summaries;
    private final ReportSampleStore samples;
    private final ReportGenerator generator;
    private final List<String> reports = new ArrayList<>();
    // Lock order: take reportLock first, then the locks inside AlertLog and UsageTracker.
    // Those classes never call back into this one, so the order cannot be reversed.
    private final Object reportLock = new Object();

    public DailyReportService(AlertLog alerts, UsageTracker usage,
                              ChamberSummaryTracker summaries, ReportSampleStore samples,
                              ReportGenerator generator) {
        this.alerts = alerts;
        this.usage = usage;
        this.summaries = summaries;
        this.samples = samples;
        this.generator = generator;
    }

    public void printDailyReport() {
        String text;
        synchronized (reportLock) {
            text = "===== DAILY REPORT =====" + System.lineSeparator()
                    + "Generated Alerts: " + alerts.getTotalGenerated() + System.lineSeparator()
                    + "Active Alerts: " + alerts.getActiveCount() + System.lineSeparator()
                    + "Water Usage: " + usage.getTotalWater() + " L" + System.lineSeparator()
                    + "Energy Usage: " + usage.getTotalEnergy() + " kWh";
            reports.add("Daily Report");
        }
        System.out.println(text);
    }

    public void archiveAlerts() {
        int moved;
        synchronized (reportLock) {
            moved = alerts.archiveActive();
            reports.add(moved > 0 ? "Archived " + moved + " alerts" : "No alerts to archive");
        }
        System.out.println(moved > 0
                ? "Archiving alerts... " + moved + " moved to archive."
                : "Archiving alerts... no active alerts to archive.");
    }

    public Report generateReport(ReportType type, Instant periodStart, Instant periodEnd) {
        return generator.generate(type, samples.getAll(), periodStart, periodEnd);
    }

    public void saveArchiveReport() {
        try {
            Files.createDirectories(ARCHIVE_DIR);
            Files.write(REPORT_FILE, buildReportLines(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            System.out.println("Archive report saved to: " + REPORT_FILE.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("Could not save archive report: " + e.getMessage());
        }
    }

    private List<String> buildReportLines() {
        List<String> archived = alerts.getArchived();
        List<String> lines = new ArrayList<>();
        lines.add("===== DAILY REPORT =====");
        lines.add("Timestamp: " + new Date());
        lines.add("Generated Alerts: " + alerts.getTotalGenerated());
        lines.add("Active Alerts: " + alerts.getActiveCount());
        lines.add("Water Usage: " + usage.getTotalWater() + " L");
        lines.add("Energy Usage: " + usage.getTotalEnergy() + " kWh");
        lines.add("Archived alerts: " + archived.size());
        lines.add("---");
        synchronized (reportLock) {
            lines.addAll(reports);
        }

        Map<String, ChamberSummary> allSummaries = summaries.getAll();
        if (!allSummaries.isEmpty()) {
            lines.add("--- chamber wise summary ---");
            for (Map.Entry<String, ChamberSummary> entry : allSummaries.entrySet()) {
                ChamberSummary summary = entry.getValue();
                lines.add(entry.getKey() + " -> Alerts=" + summary.getAlerts()
                        + ", Actions=" + summary.getMitigationActions()
                        + ", Last Action=" + summary.getLastAction()
                        + ", Last Alert=" + summary.getLastAlert());
            }
        }

        if (!archived.isEmpty()) {
            lines.add("--- archived alerts ---");
            lines.addAll(archived);
        }

        Report environmental = generateReport(ReportType.DAILY_ENVIRONMENTAL,
                Instant.EPOCH, Instant.now());
        lines.add("--- environmental report ---");
        lines.addAll(environmental.lines());
        return lines;
    }
}
