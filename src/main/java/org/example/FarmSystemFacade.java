package org.example;

import org.example.alert.AlertListener;
import org.example.alert.AlertLog;
import org.example.alert.AlertPrinter;
import org.example.alert.LegacyCompatibleAlertEvaluator;
import org.example.alert.ReadingAlertEvaluator;
import org.example.crop.CropProfile;
import org.example.crop.CropRepository;
import org.example.crop.DefaultCropProfiles;
import org.example.crop.InMemoryCropRepository;
import org.example.device.ControlQueue;
import org.example.device.DeviceAlertHandler;
import org.example.device.DeviceController;
import org.example.device.DeviceWorker;
import org.example.report.ChamberSummaryTracker;
import org.example.report.DailyReportService;
import org.example.report.Report;
import org.example.report.ReportSampleListener;
import org.example.report.ReportSampleStore;
import org.example.report.ReportType;
import org.example.report.StandardReportGenerator;
import org.example.sensor.ChamberMonitor;
import org.example.sensor.ChamberStateListener;
import org.example.sensor.ChamberStateStore;
import org.example.sensor.ReadingListener;
import org.example.sensor.SensorFactory;
import org.example.sensor.SensorLogListener;
import org.example.usage.CycleUsageListener;
import org.example.usage.DefaultUsageRates;
import org.example.usage.UsageTracker;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Facade: connects the crop, sensor, alert, device, usage and report components
 * so callers only need this class.
 */
public class FarmSystemFacade {
    private static final int CONTROL_QUEUE_CAPACITY = 5;
    private static final int CYCLES_PER_CHAMBER = 20;

    private final Random random = new Random();
    private final CropRepository crops = new InMemoryCropRepository();
    private final ChamberStateStore states = new ChamberStateStore();
    private final SensorLogListener sensorLog = new SensorLogListener();
    private final AlertLog alertLog = new AlertLog();
    private final UsageTracker usage = new UsageTracker(DefaultUsageRates.create());
    private final ChamberSummaryTracker summaries = new ChamberSummaryTracker();
    private final ControlQueue controlQueue = new ControlQueue(CONTROL_QUEUE_CAPACITY);
    private final DeviceWorker deviceWorker = new DeviceWorker(
            controlQueue, new DeviceController(), states, summaries);
    private final ReportSampleStore samples = new ReportSampleStore();
    private final DailyReportService reports = new DailyReportService(
            alertLog, usage, summaries, samples, new StandardReportGenerator());
    private final ReadingAlertEvaluator evaluator = new LegacyCompatibleAlertEvaluator();
    // Handler order matches legacy: summary, alert list and printout, usage, then device request.
    private final AlertListener alertListener = new AlertListener(crops, evaluator, List.of(
            summaries, alertLog, new AlertPrinter(), usage, new DeviceAlertHandler(controlQueue)));
    private final ChamberStateListener stateListener = new ChamberStateListener(states);
    private final List<ChamberMonitor> chamberMonitors = new ArrayList<>();

    public FarmSystemFacade() {
        DefaultCropProfiles.loadInto(crops);
    }

    public void addCrop(CropProfile profile) {
        crops.save(profile);
    }

    public boolean removeCrop(String name) {
        return crops.remove(name);
    }

    public void addChamber(String chamber, String crop) {
        chamberMonitors.add(new ChamberMonitor(chamber, crop,
                SensorFactory.createChamberSensors(random), createChamberListeners(),
                CYCLES_PER_CHAMBER));
    }

    public void runSimulation() throws InterruptedException {
        // One thread per chamber plus one for the device worker. With fewer, the worker could
        // wait for a thread while chambers wait on a full queue, and neither would ever finish.
        ExecutorService pool = Executors.newFixedThreadPool(chamberMonitors.size() + 1);
        try {
            Future<?> deviceTask = pool.submit(deviceWorker);
            List<Future<?>> chamberTasks = new ArrayList<>();
            for (ChamberMonitor monitor : chamberMonitors) {
                chamberTasks.add(pool.submit(monitor));
            }
            for (Future<?> chamberTask : chamberTasks) {
                waitFor(chamberTask);
            }
            controlQueue.shutdown();
            waitFor(deviceTask);

            Future<?> archiveTask = pool.submit(reports::archiveAlerts);
            Future<?> reportTask = pool.submit(reports::printDailyReport);
            waitFor(reportTask);
            waitFor(archiveTask);
        } finally {
            pool.shutdown();
        }
        System.out.println("System finished sensor processing.");
        reports.saveArchiveReport();
    }

    private void waitFor(Future<?> task) throws InterruptedException {
        try {
            task.get();
        } catch (ExecutionException e) {
            System.out.println("A task failed: " + e.getCause());
        }
    }

    public Report generateReport(ReportType type, Instant periodStart, Instant periodEnd) {
        return reports.generateReport(type, periodStart, periodEnd);
    }

    public void recordIrrigationCycle() {
        usage.recordIrrigationCycle();
    }

    public void recordFeedingCycle() {
        usage.recordFeedingCycle();
    }

    public void showAlerts() {
        for (String alert : alertLog.getActive()) {
            System.out.println(alert);
        }
    }

    public void showLogs() {
        for (String log : sensorLog.getLogs()) {
            System.out.println(log);
        }
    }

    // Order matches legacy: log, save state, check alerts, then close the cycle.
    // Cycle usage must come before the sample, so the sample includes it.
    List<ReadingListener> createChamberListeners() {
        return List.of(sensorLog, stateListener, alertListener,
                new CycleUsageListener(usage), new ReportSampleListener(samples, usage));
    }

    // The accessors below let the behaviour comparison in scripts/compare read results.
    SensorLogListener sensorLog() {
        return sensorLog;
    }

    ChamberStateStore states() {
        return states;
    }

    AlertLog alertLog() {
        return alertLog;
    }

    UsageTracker usage() {
        return usage;
    }

    ChamberSummaryTracker summaries() {
        return summaries;
    }

    ControlQueue controlQueue() {
        return controlQueue;
    }

    DeviceWorker deviceWorker() {
        return deviceWorker;
    }
}
