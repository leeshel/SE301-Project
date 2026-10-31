package org.example;

import org.example.alert.Alert;
import org.example.alert.AlertLog;
import org.example.alert.AlertSeverity;
import org.example.alert.AlertType;
import org.example.device.AutomatedDevice;
import org.example.device.ControlQueue;
import org.example.device.ControlRequest;
import org.example.device.DeviceController;
import org.example.report.ChamberSummary;
import org.example.report.ChamberSummaryTracker;
import org.example.report.ReportType;
import org.example.sensor.ChamberState;
import org.example.sensor.ChamberStateStore;
import org.example.sensor.SensorType;
import org.example.usage.UsageTracker;

import java.io.OutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hammers each shared component from many threads and checks that no update was lost.
 * Prints PASS or FAIL per check. Run from a scratch directory: it writes archive/.
 */
public class ConcurrencyCheck {
    private static final int THREADS = 8;
    private static final long TIMEOUT_MILLIS = 120_000;
    private static final PrintStream OUT = System.out;

    interface Work {
        void run(int thread) throws Exception;
    }

    public static void main(String[] args) throws Exception {
        int rounds = args.length > 0 ? Integer.parseInt(args[0]) : 200;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        check("AlertLog", ConcurrencyCheck::alertLog);
        check("UsageTracker", ConcurrencyCheck::usageTracker);
        check("ChamberStateStore", ConcurrencyCheck::chamberStateStore);
        check("Device read-then-write", ConcurrencyCheck::deviceReadThenWrite);
        check("ChamberSummaryTracker", ConcurrencyCheck::chamberSummary);
        check("ControlQueue", ConcurrencyCheck::controlQueue);
        check("Full app", () -> fullRuns(rounds));
    }

    interface Check {
        void run() throws Exception;
    }

    // A race can corrupt a collection badly enough that reading the result throws.
    private static void check(String name, Check check) {
        try {
            check.run();
        } catch (Exception e) {
            OUT.println("FAIL " + name + ": corrupted, reading the result threw " + e);
        }
    }

    private static void alertLog() throws Exception {
        AlertLog log = new AlertLog();
        Alert alert = new Alert("CH1", "Tomato", AlertType.TEMPERATURE, AlertSeverity.WARNING,
                "High Temp", Instant.now());
        int each = 50_000;
        String crash = hammer(THREADS, thread -> {
            for (int i = 0; i < each; i++) {
                log.onAlert(alert);
                if (i % 1000 == 0) {
                    log.archiveActive();
                }
            }
        });
        log.archiveActive();
        int expected = THREADS * each;
        report("AlertLog", crash, "generated=" + log.getTotalGenerated() + " archived="
                + log.getArchived().size() + " expected=" + expected,
                log.getTotalGenerated() == expected && log.getArchived().size() == expected);
    }

    private static void usageTracker() throws Exception {
        UsageTracker usage = new UsageTracker(Map.of());
        int each = 200_000;
        String crash = hammer(THREADS, thread -> {
            for (int i = 0; i < each; i++) {
                usage.recordCycle("CH" + thread);
            }
        });
        double pending = 0;
        for (int t = 0; t < THREADS; t++) {
            pending += usage.takeChamberUsage("CH" + t).waterLitres();
        }
        int expected = THREADS * each * 2;
        report("UsageTracker", crash, "water=" + usage.getTotalWater() + " pending=" + pending
                + " expected=" + expected, usage.getTotalWater() == expected && pending == expected);
    }

    private static void chamberStateStore() throws Exception {
        int rounds = 2000;
        int wrong = 0;
        String crash = null;
        for (int round = 0; round < rounds; round++) {
            ChamberStateStore store = new ChamberStateStore();
            Set<ChamberState> seen = ConcurrentHashMap.newKeySet();
            String result = hammer(THREADS, thread -> seen.add(store.getOrCreateState("CH1")));
            crash = result == null ? crash : result;
            wrong += seen.size() == 1 ? 0 : 1;
        }
        report("ChamberStateStore", crash, "rounds with more than one state=" + wrong, wrong == 0);
    }

    private static void deviceReadThenWrite() throws Exception {
        DeviceController controller = new DeviceController();
        controller.registerDevice(new AutomatedDevice() {
            public String getDeviceName() {
                return "Adder";
            }

            public String activate(ChamberState state) {
                state.setValue(SensorType.TEMPERATURE, state.getValue(SensorType.TEMPERATURE) + 1);
                return "";
            }
        });
        ChamberState state = new ChamberState();
        state.setValue(SensorType.TEMPERATURE, 0);
        int each = 200_000;
        String crash = hammer(THREADS, thread -> {
            for (int i = 0; i < each; i++) {
                controller.activateDevice("Adder", state);
            }
        });
        double value = state.getValue(SensorType.TEMPERATURE);
        report("Device read-then-write", crash, "value=" + value + " expected=" + THREADS * each,
                value == THREADS * each);
    }

    private static void chamberSummary() throws Exception {
        ChamberSummaryTracker tracker = new ChamberSummaryTracker();
        int each = 200_000;
        String crash = hammer(THREADS, thread -> {
            for (int i = 0; i < each; i++) {
                tracker.recordAction("CH" + (i % 4), "Fan");
            }
        });
        int total = 0;
        for (ChamberSummary summary : tracker.getAll().values()) {
            total += summary.getMitigationActions();
        }
        report("ChamberSummaryTracker", crash, "actions=" + total + " expected=" + THREADS * each,
                total == THREADS * each);
    }

    private static void controlQueue() throws Exception {
        ControlQueue queue = new ControlQueue(5);
        int each = 50_000;
        int[] taken = {0};
        Thread consumer = new Thread(() -> {
            while (queue.take() != null) {
                taken[0]++;
            }
        });
        consumer.start();
        String crash = hammer(THREADS, thread -> {
            for (int i = 0; i < each; i++) {
                queue.put(new ControlRequest("CH" + thread, "Fan"));
            }
        });
        queue.shutdown();
        consumer.join(TIMEOUT_MILLIS);
        report("ControlQueue", crash, "taken=" + taken[0] + " expected=" + THREADS * each,
                !consumer.isAlive() && taken[0] == THREADS * each);
    }

    private static void fullRuns(int rounds) throws Exception {
        int bad = 0;
        String firstBad = "";
        for (int round = 0; round < rounds; round++) {
            FarmSystemFacade farm = new FarmSystemFacade();
            String[] crops = {"Tomato", "Strawberry", "Lettuce", "Chili"};
            for (int c = 0; c < 8; c++) {
                farm.addChamber("CH" + c, crops[c % 4]);
            }
            String crash = hammer(1, thread -> farm.runSimulation());
            int summaryAlerts = 0;
            for (ChamberSummary summary : farm.summaries().getAll().values()) {
                summaryAlerts += summary.getAlerts();
            }
            int generated = farm.alertLog().getTotalGenerated();
            int archived;
            try {
                archived = farm.alertLog().getArchived().size();
            } catch (NullPointerException e) {
                archived = -1;
            }
            int water = farm.usage().getTotalWater();
            int energy = farm.usage().getTotalEnergy();
            double reportWater = total(farm, ReportType.WATER_USAGE);
            double reportEnergy = total(farm, ReportType.ENERGY_CONSUMPTION);
            boolean ok = crash == null && generated == archived && generated == summaryAlerts
                    && water == reportWater && energy == reportEnergy;
            if (!ok) {
                bad++;
                if (firstBad.isEmpty()) {
                    firstBad = " first: generated=" + generated + " archived=" + archived
                            + " summaries=" + summaryAlerts + " water=" + water + "/" + reportWater
                            + " energy=" + energy + "/" + reportEnergy + " crash=" + crash;
                }
            }
        }
        report("Full app, " + rounds + " runs", null, "runs with wrong totals=" + bad + firstBad, bad == 0);
    }

    private static double total(FarmSystemFacade farm, ReportType type) {
        for (String line : farm.generateReport(type, Instant.EPOCH, Instant.now()).lines()) {
            if (line.startsWith("Total: ")) {
                return Double.parseDouble(line.split(" ")[1]);
            }
        }
        throw new IllegalStateException("No total in " + type);
    }

    // Runs the work on several threads. Returns null, or a description of a crash or hang.
    private static String hammer(int threads, Work work) throws Exception {
        List<Thread> started = new ArrayList<>();
        Set<String> crashes = ConcurrentHashMap.newKeySet();
        for (int t = 0; t < threads; t++) {
            int thread = t;
            Thread worker = new Thread(() -> {
                try {
                    work.run(thread);
                } catch (Exception e) {
                    crashes.add(e.getClass().getSimpleName());
                }
            });
            worker.setDaemon(true);
            worker.start();
            started.add(worker);
        }
        for (Thread worker : started) {
            worker.join(TIMEOUT_MILLIS);
            if (worker.isAlive()) {
                return "HANG";
            }
        }
        return crashes.isEmpty() ? null : "crashed with " + crashes;
    }

    private static void report(String name, String crash, String detail, boolean ok) {
        OUT.println((ok && crash == null ? "PASS " : "FAIL ") + name + ": " + detail
                + (crash == null ? "" : " (" + crash + ")"));
    }
}
