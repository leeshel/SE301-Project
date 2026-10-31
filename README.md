# SE301-Project

Singapore Vertical Farming Research Management System (SVFRMS): a refactor of a legacy single-file Java application into separate components. Behaviour is preserved except for the deliberate changes listed in section 4.3.

Links to legacy line numbers open the [original `SVFRMS.java`](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java) as it was before the refactor (commit `532a86d`).

## Analysis of the Original System and Identified Issues

### What the original system does

Each growing chamber runs in its own thread and reads five simulated sensors for 20 cycles. Each reading is checked against the crop's limits. A reading outside them creates an alert, adds to the water and energy totals, and usually queues a device request for a separate control thread. At the end, the system archives the alerts and writes a daily report.

All of this was in one class, `SVFRMS`, in 393 lines.

### Identified issues

| Issue | Legacy example | Why it matters |
| --- | --- | --- |
| **One method has many responsibilities** | `processSensor` ([141-205](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L141-L205)) logs, saves state, checks limits, creates alerts, adds usage and requests devices | Any change risks the other jobs, and they cannot be tested separately (Single Responsibility) |
| **Hardcoded crop rules** | `if(crop.equals("Tomato"))` chains ([147-170](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L147-L170)) | A new crop means editing and re-testing this method (Open/Closed) |
| **Behaviour chosen by text values** | `handleEvent` ([62-88](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L62-L88)) and `activateControl` ([90-129](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L90-L129)) | A misspelt value is silently ignored, and each new device lengthens the chain |
| **Global mutable state** | Public static lists, maps and counters ([10-24](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L10-L24)) | Any code can change any data, and thread access is uncontrolled |
| **No interfaces** | Everything is a static method on one concrete class | Parts cannot be replaced or tested alone (Dependency Inversion) |
| **Incomplete features** | Only one daily summary exists, and only some crops have humidity and soil checks | Reports and limits in the specification are missing |
| **Race conditions** | Unlocked `HashMap`, `ArrayList` and `int` updates ([48-53](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L48-L53), [73-74](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L73-L74), [291-292](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L291-L292)) | Crashes and lost data: 149 of 300 legacy runs crashed a thread |
| **Hang risk** | Chambers wait on a full queue with no recovery if the control thread dies ([133-135](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L133-L135)) | The application could stop permanently |

The legacy code has **no lock-order deadlock**: `generateDailyReport` and `archiveAlerts` both take `alertLock` before `reportLock`. `waterPumpLock` and `nutrientLock` are declared but never used.

### How the code was split

Each job from the legacy class now belongs to its own class, and `FarmSystemFacade` connects them so callers do not need to know about them.

| Legacy code | Job | New class |
| --- | --- | --- |
| `processSensor` | Log the reading | `SensorLogListener` |
| `processSensor` | Save the latest value | `ChamberStateListener`, `ChamberStateStore` |
| `processSensor` | Check limits | `AlertListener`, one `AlertRule` class per alert type, `CropProfile` |
| `processSensor`, `handleEvent` | Record and print alerts | `AlertLog`, `AlertPrinter`, `ChamberSummaryTracker` |
| `processSensor`, `ChamberThread` | Add water and energy usage | `UsageTracker`, `CycleUsageListener` |
| `processSensor`, `addControlRequest`, `ControlThread` | Queue and run device requests | `DeviceAlertHandler`, `ControlQueue`, `DeviceWorker` |
| `activateControl` | Change chamber values | `AutomatedDevice` and one class per device |
| `generateDailyReport`, `archiveAlerts`, `saveArchiveReport` | Build reports | `DailyReportService`, `StandardReportGenerator` |
| `ChamberThread` | Read sensors | `ChamberMonitor`, `Sensor`, `SensorFactory` |
| `main` | Start the system | `SVFRMS`, `FarmSystemFacade` |

### Simplified folder structure

```text
src/main/java/org/example/
├── SVFRMS.java            # Entry point: lists the chambers and starts the system
├── FarmSystemFacade.java  # Facade: connects all components
├── crop/                  # Crop profiles and how they are stored
├── sensor/                # Sensors, chamber monitoring, logs and chamber state
├── alert/                 # Alert log and printing
│   └── alert_rules/       # One class per alert type
├── device/                # Control queue and device thread
│   └── device_types/      # One class per device
├── usage/                 # Water, energy and nutrient usage
└── report/                # Report data, generation and chamber summaries
    └── report_types/      # One class per report
scripts/                   # Behaviour comparison and stress tests
```

### How a reading flows through the system

```text
ChamberMonitor reads the sensors
   ├─ SensorLogListener     writes the log
   ├─ ChamberStateListener  saves the value
   ├─ AlertListener         runs each AlertRule, then for each alert:
   │     ├─ ChamberSummaryTracker  updates the chamber summary
   │     ├─ AlertLog               stores the alert
   │     ├─ AlertPrinter           prints the alert
   │     ├─ UsageTracker           adds water and energy
   │     └─ DeviceAlertHandler     queues a request → ControlQueue → DeviceWorker → AutomatedDevice
   ├─ CycleUsageListener    adds the fixed usage once per cycle
   └─ ReportSampleListener  saves one report sample per cycle
```

## Directory Layout

### `src/main/java/org/example`

```text
org/example/
├── FarmSystemFacade.java # Facade that connects all components.
└── SVFRMS.java           # Entry point that lists the chambers and starts the system.
```

### `src/main/java/org/example/alert`

```text
alert/
├── Alert.java         # Immutable alert result.
├── alert_rules/       # One class per alert type; see below.
├── AlertHandler.java  # Interface for reacting to an alert.
├── AlertListener.java # Runs the rules on each reading and passes alerts to the handlers.
├── AlertLog.java      # Stores active and archived alerts.
├── AlertPrinter.java  # Prints each alert to the console.
├── AlertSeverity.java # Defines INFO, WARNING, and CRITICAL levels.
└── AlertType.java     # Enumerates supported alert categories.
```

### `src/main/java/org/example/alert/alert_rules`

```text
alert/alert_rules/
├── AlertChecks.java            # Range and minimum checks shared by the rules.
├── AlertRule.java              # Interface for one type of alert check.
├── DefaultAlertRules.java      # Lists the alert rules the application uses.
├── HumidityRule.java           # Alerts when humidity is above or below the crop's range.
├── LowLightRule.java           # Alerts when light is below 500 lux.
├── LowSoilMoistureRule.java    # Alerts when soil moisture is below the crop's minimum.
├── NutrientDeficiencyRule.java # Alerts when nutrients are below 40%.
├── SensorFailureRule.java      # Alerts when a sensor reports an invalid value.
└── TemperatureRule.java        # Alerts when temperature is above or below the crop's range.
```

### `src/main/java/org/example/crop`

```text
crop/
├── CropBuilder.java            # Builds crop profiles with optional ranges.
├── CropProfile.java            # Immutable crop name and environmental ranges.
├── CropRepository.java         # Interface for saving, finding, and removing crops.
├── DefaultCropProfiles.java    # Starting data for the six existing crops.
├── InMemoryCropRepository.java # Thread-safe crop storage.
└── Range.java                  # Immutable minimum and maximum values.
```

### `src/main/java/org/example/device`

```text
device/
├── ControlQueue.java       # Bounded queue of device requests.
├── ControlRequest.java     # Immutable request for one device in one chamber.
├── device_types/           # One class per device; see below.
├── DeviceAlertHandler.java # Maps each alert to a device request.
├── DeviceController.java   # Finds a device by name and activates it.
└── DeviceWorker.java       # Runs queued device requests on its own thread.
```

### `src/main/java/org/example/device/device_types`

```text
device/device_types/
├── AutomatedDevice.java   # Interface for a device that changes a chamber.
├── CoolingFan.java        # Reduces temperature.
├── Dehumidifier.java      # Reduces humidity.
├── GrowLight.java         # Increases light level.
├── HeatingUnit.java       # Increases temperature.
├── Humidifier.java        # Increases humidity.
├── NutrientInjector.java  # Increases nutrient level.
├── ShadeController.java   # Reduces light level.
├── VentilationSystem.java # Reduces humidity through air circulation.
└── WaterPump.java         # Increases soil moisture.
```

### `src/main/java/org/example/report`

```text
report/
├── ChamberSummary.java          # Alert and action counts for one chamber.
├── ChamberSummaryTracker.java   # Keeps one summary per chamber.
├── DailyReportService.java      # Prints, archives, and saves the daily report.
├── Report.java                  # Immutable completed report with text output.
├── report_types/                # One class per report; see below.
├── ReportGenerator.java         # Interface for generating reports.
├── ReportSample.java            # Validated environmental and resource-use data.
├── ReportSampleListener.java    # Saves one report sample per chamber cycle.
├── ReportSampleStore.java       # Thread-safe list of report samples.
├── ReportType.java              # Enumerates the five supported report types.
└── StandardReportGenerator.java # Selects the report strategy and filters samples.
```

### `src/main/java/org/example/report/report_types`

```text
report/report_types/
├── DailyEnvironmentalReport.java  # Builds the daily environmental report.
├── DefaultReportStrategies.java   # Lists the five supported reports.
├── EnergyConsumptionReport.java   # Builds the energy consumption report.
├── NutrientConsumptionReport.java # Builds the nutrient consumption report.
├── ReportLines.java               # Logic shared by the reports.
├── ReportStrategy.java            # Interface for building one type of report.
├── WaterUsageReport.java          # Builds the water usage report.
└── WeeklyCropReport.java          # Builds the weekly crop report.
```

### `src/main/java/org/example/sensor`

```text
sensor/
├── ChamberMonitor.java       # Reads a chamber's sensors and publishes each reading.
├── ChamberState.java         # Thread-safe latest values for one chamber.
├── ChamberStateListener.java # Saves each reading to the chamber state.
├── ChamberStateStore.java    # Keeps one state per chamber.
├── ReadingListener.java      # Interface for reacting to a sensor reading.
├── Sensor.java               # Interface for reading a sensor.
├── SensorFactory.java        # Creates the sensors for a chamber.
├── SensorLogListener.java    # Records a log entry for each reading.
├── SensorReading.java        # Immutable single sensor value.
├── SensorType.java           # Enumerates the five sensor types.
└── SimulatedSensor.java      # Produces random values within a range.
```

### `src/main/java/org/example/usage`

```text
usage/
├── ChamberUsage.java       # Immutable usage for one chamber.
├── CropUsageRates.java     # Immutable extra usage amounts for one crop.
├── CycleUsageListener.java # Records the fixed usage once per chamber cycle.
├── DefaultUsageRates.java  # Starting usage amounts per crop.
└── UsageTracker.java       # Keeps water, energy, and nutrient usage.
```

### `scripts`

```text
scripts/
├── alert-report-smoketest.jsh # Exercises the standard alert rules and reports.
├── compare/                   # Inputs and adapters for the behaviour comparison.
├── compare-with-legacy.sh     # Compares behaviour with the legacy code.
└── concurrency/               # Stress test for shared components.
```

## How to Run

**Requirements:** Java 25 or later, and Maven.

```sh
mvn package
java -jar target/svfrms-1.0.0.jar
```

The application prints alerts and device actions as it runs, then saves its report to `archive/daily_report.txt` in the folder you ran it from. Readings are random, so the numbers change on each run.

**Compare behaviour with the legacy code.** This needs the git history. Only Lettuce humidity, Lettuce soil moisture and Strawberry soil moisture should differ:

```sh
./scripts/compare-with-legacy.sh
```

**Run the concurrency stress test.** Each line prints `PASS` or `FAIL`. The test writes to `archive/`, so run it from a scratch folder:

```sh
mkdir -p /tmp/svfrms-check
javac -d /tmp/svfrms-check/classes $(find src/main/java -name '*.java') scripts/concurrency/org/example/ConcurrencyCheck.java
(cd /tmp/svfrms-check && java -cp classes org.example.ConcurrencyCheck 200)
```

**Add a new alert, report or device.** Add a class to `alert/alert_rules/`, `report/report_types/` or `device/device_types/`, then one line to `DefaultAlertRules`, `DefaultReportStrategies` or `DeviceController`. A new alert or report also needs a value in `AlertType` or `ReportType`.

**Add a new crop.** Add one entry to `DefaultCropProfiles`, then assign it to a chamber in `SVFRMS.java`:

```java
repository.save(new CropBuilder("Kale")
        .temperatureRange(15, 24)
        .humidityRange(50, 70)
        .soilMoistureRange(40, 60)
        .lightLevelRange(MIN_LIGHT_LUX, MAX_LIGHT_LUX)
        .build());
```

```java
farm.addChamber("CH5", "Kale");
```

## 1. Crop Profile Management

### 1.1 Legacy problems and fixes

| Method and lines                                                                                                                                                                                    | Problem                                                                                                                                              | Implication                                                                                                                | Fix applied                                                                                                                                               |
| --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `processSensor` ([147-170](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L147-L170), [174-181](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L174-L181), [185-187](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L185-L187)) | Crop thresholds are hardcoded in `if/else` chains                                                                                                  | Adding or removing a crop means editing this method, which breaks the open/closed principle and risks breaking other crops | Thresholds move into `CropProfile` objects, created with `CropBuilder`. Adding a crop is one `save(...)` call                                       |
| `processSensor` (same lines)                                                                                                                                                                      | Crop names are repeated strings, such as `crop.equals("Tomato")`                                                                                   | A misspelt or unknown crop matches no branch, so its checks are skipped with no error                                      | `CropRepository.findByName` returns an empty result for an unknown crop, so it can be detected                                                          |
| `processSensor` (same lines)                                                                                                                                                                      | Threshold numbers are mixed into the logic and copied into each crop's branch                                                                        | Values are hard to find, compare or change, and there is no check that a minimum is below its maximum                      | `DefaultCropProfiles` holds the starting data in one place, and `Range` rejects `min > max`                                                         |
| `processSensor` ([174-187](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L174-L187), [191](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L191))                                                                     | Crop data is incomplete: humidity covers only Tomato and Strawberry, soil moisture only Tomato, and light is one fixed 500 lux minimum for all crops | The code does not store the ranges required by section 3.1 of the specification                                            | `CropProfile` has all four ranges. Spinach, Chili and Cucumber leave humidity and soil moisture empty because no source defines them                    |
| Whole class                                                                                                                                                                                         | There is no crop concept and no way to add, find or remove a crop                                                                                    | Crops cannot be managed, and new crops need code changes (section 4.5)                                                     | `CropRepository` provides `save`, `remove`, `findByName` and `findAll`, and other code depends on this interface rather than the implementation |

### 1.2 Concurrency

The legacy crop data is fixed in code, so it has no race condition of its own. The refactor introduces shared crop data, which is protected as follows:

| Class                      | Protection            | Why it works                                                                 |
| -------------------------- | --------------------- | ---------------------------------------------------------------------------- |
| `CropProfile`, `Range` | Immutable             | Values cannot change after creation, so threads can share them without locks |
| `InMemoryCropRepository` | `ConcurrentHashMap` | Saves, removes and lookups are safe while chamber threads read               |

**Deadlock:** none is possible in this package, because its code uses no explicit locks.

### 1.3 Design decisions

- **Builder pattern:** makes profile creation readable, and adding an optional range later does not break existing calls.
- **Crops are data, not subclasses:** Kale and Tomato behave the same and differ only in their values.
- **`CropProfile` is `final`:** a subclass could add changeable state and undo the thread-safety guarantee.
- **Light range is inferred:** the specification gives no light values, so 500-1000 lux comes from the legacy 500 lux alert threshold and the Grow Light's 1000 lux cap ([line 124](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L124)).

## 2. Environment Sensors

### 2.1 Legacy design problems and fixes

| Method and lines                                                                                                                                                                                                                                                             | Problem                                                                                            | Implication                                                                                                             | Fix applied                                                                                                                                                                                                         |
| ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `processSensor` ([141-205](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L141-L205))                                                                                                                                                                                                | One method does logging, state updates, threshold checks, alerts, usage totals and device requests | It breaks the single responsibility principle, so any change risks the other jobs, and they cannot be tested separately | **Observer pattern:** `ChamberMonitor` publishes each `SensorReading` to `ReadingListener` objects, with one listener per job. Logging, state, alerts, usage and report samples each have their own listener |
| `processSensor` ([145](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L145), [172](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L172), [183](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L183), [189](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L189), [197](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L197)) | Sensor types are strings compared in an `if/else` chain                                          | A misspelt type is silently ignored, and a new sensor means editing the chain                                           | `SensorType` enum, so the compiler catches invalid types                                                                                                                                                          |
| `ChamberThread.run` ([285-293](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L285-L293))                                                                                                                                                                                            | Sensor value ranges are hardcoded inside the thread loop                                           | Sensors cannot be replaced or configured, and real hardware would mean rewriting the loop                               | `Sensor` interface, `SimulatedSensor` implementation, and **Factory pattern** through `SensorFactory`                                                                                                   |
| `ChamberThread` ([275](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L275))                                                                                                                                                                                                         | The class extends `Thread`, mixing the work with how it runs                                     | The monitoring work is tied to one way of running it                                                                    | `ChamberMonitor` implements `Runnable`, depends only on the `Sensor` and `ReadingListener` interfaces, and runs in a fixed thread pool                                                                                                    |
| `random` ([line 31](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L31))                                                                                                                                                                                                             | All chambers share one static `Random`. This is thread-safe, so it is not a race condition       | Threads compete for it, and tests cannot control its values                                                             | `SensorFactory` takes a `Random` from the caller, so tests can use a fixed seed. `FarmSystemFacade` shares one between chambers, which is thread-safe                                                                                               |

### 2.2 Race conditions and fixes

| Method and lines                                                                                                                                                                                                                                                                                                                                              | Race condition                                                                                            | Implication                                                                                                                                      | Fix applied                                                                                            | Status                                                                               |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------ |
| `getChamberState` ([48-53](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L48-L53))                                                                                                                                                                                                                                                                                   | Check-then-insert on a shared `HashMap` with no lock, so an insert can be lost and `get` returns null | `NullPointerException` kills the chamber thread, for example at [line 146](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L146), and it stops processing | `ChamberStateStore` uses `ConcurrentHashMap.putIfAbsent`, which checks and inserts as one step | Fixed                                                                                |
| `processSensor` ([146](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L146), [173](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L173), [184](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L184), [190](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L190), [198](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L198)) vs `activateControl` ([94-124](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L94-L124)) | Chamber threads overwrite values while the control thread reads and changes them, with no lock | Sensor updates or device adjustments can be lost, leaving wrong chamber values | `ChamberState` uses `synchronized` blocks, and `DeviceController` holds the same lock across each device's read and write | Fixed |
| `handleEvent` ([line 64](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L64))                                                                                                                                                                                                                                                                                         | Chamber threads add to a shared `ArrayList` with no lock                                                | Log entries can be lost                                                                                                                          | `SensorLogListener` uses a synchronized block, with the text built outside it for liveness           | Fixed                                                                                |
| `getChamberSummary` ([55-60](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L55-L60)) | Same check-then-insert race as `getChamberState` | Lost summaries and possible `NullPointerException` | `ChamberSummaryTracker` uses `ConcurrentHashMap.putIfAbsent` | Fixed |

### 2.3 Deadlock

- **Legacy:** `processSensor` can block in `addControlRequest` when the control queue is full ([133-135](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L133-L135)), which becomes a permanent hang if the control thread dies. Section 3.2 covers the fix.
- **Refactored:** this package cannot deadlock. `ChamberState` and `SensorLogListener` each take one lock, and neither calls listeners or other outside code while holding it.

### 2.4 Design decisions

- **One `SimulatedSensor` class:** legacy sensors differ only in their value range, so five subclasses would repeat the same code.
- **Small interfaces:** `Sensor` and `ReadingListener` each have one focused purpose, instead of one large `EnvironmentSensors` interface (interface segregation).
- **Listener order matters:** `FarmSystemFacade` registers logging, state, alerts, cycle usage, then report samples. This matches the legacy order, and a sample must include that cycle's usage.
- **Shared listeners must be thread-safe:** chamber threads share the log, state and alert listeners. `CycleUsageListener` and `ReportSampleListener` are created per chamber, so their cycle data stays in one thread.

## 3. Automated Devices

### 3.1 Legacy design problems and fixes

| Method and lines | Problem | Implication | Fix applied |
| --- | --- | --- | --- |
| `activateControl` ([90-129](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L90-L129)) | One `if/else` chain holds every device's behaviour | A new device means editing this method (Open/Closed) | `AutomatedDevice` interface with one class per device, and `DeviceController` finds a device by name |
| `addControlRequest`, `ControlThread` ([131-139](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L131-L139), [251-273](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L251-L273)) | Requests are text such as `"CH1\|Water Pump"`, and queue logic is mixed into the main class | Text must be split again, and the queue cannot be tested alone | `ControlRequest` record, `ControlQueue` class and `DeviceWorker` |
| `processSensor` ([141-205](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L141-L205)) | Choosing a device is mixed with alert logic | Device rules cannot change without touching alert rules | `DeviceAlertHandler` maps each alert to a device |

### 3.2 Concurrency

| Class or method | Issue | Fix applied |
| --- | --- | --- |
| Devices | Each device reads a value and writes it back, so another thread's write in between is lost | `DeviceController` holds the chamber state's lock across the whole action, using a `synchronized` block |
| `ControlQueue` | Producers and the consumer share one list | `synchronized` blocks on a private lock, with `wait` and `notifyAll` while full or empty |
| `DeviceWorker` | Legacy: if the control thread dies, chambers wait forever on a full queue | The worker logs a failed request and keeps running |
| Thread pool | Too few threads could leave the worker without one, while chambers wait on a full queue | `FarmSystemFacade` uses `newFixedThreadPool(chambers + 1)` |

### 3.3 Design decisions

- **Strategy pattern:** each `AutomatedDevice` is a different way to change a chamber, and `DeviceController` runs it without knowing which one it is.
- **Devices change the state they receive:** changing the chamber is the device's whole job, so `activate(ChamberState)` is a deliberate exception to avoiding output arguments.
- **Humidifier and Dehumidifier are kept:** the legacy code uses them, although the specification's list does not include them.
- **Heating Unit, Ventilation System and Shade Controller are available but not triggered:** the legacy code never requests them, so triggering them would change behaviour.

## 4. Alert Management

### 4.1 Legacy design problems and fixes

| Method and lines | Problem | Implication | Fix applied |
| --- | --- | --- | --- |
| `processSensor` ([147-205](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L147-L205)) | Alert rules, usage and device requests are written together for each crop | Rules are repeated and hard to change | One `AlertRule` class per alert type decides, then separate `AlertHandler` classes react |
| `processSensor` ([147-170](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L147-L170)) | Limits are hardcoded per crop | New crops need new alert code | Rules read limits from `CropProfile` |
| `handleEvent` ([62-88](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L62-L88)) | The action is chosen by text such as `"CREATE_ALERT"` | A misspelt value is silently ignored | `Alert` record with `AlertType` and `AlertSeverity` enums, which the compiler checks. Other classes act on the type, never on message text |
| `alerts`, `archivedAlerts` ([10-11](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L10-L11)) | Public static lists | Any code can change them without a lock | `AlertLog` keeps them private |

### 4.2 Concurrency

| Class | Issue in legacy | Fix applied |
| --- | --- | --- |
| `AlertLog` | `alerts.add` and `totalAlerts++` ran with no lock ([73-74](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L73-L74)) | A `synchronized` block protects the lists and counter; printing is in `AlertPrinter`, outside the lock |
| `ChamberSummary` | Summary counters were updated with no lock ([80](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L80), [85](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L85)) | `synchronized` blocks on a private lock |
| Alert rules, `Alert` | Not applicable | They hold no changing data, so threads can share them without locks |

### 4.3 Deliberate behaviour changes

Alert limits now come from crop profiles, which include ranges from section 3.1 of the specification that the legacy code never checked. The comparison test confirms these are the only differences in alerts:

| Crop and sensor | Legacy | Now |
| --- | --- | --- |
| Strawberry, soil moisture | No alert | Alert below 50%, Water Pump request and 50 L of water |
| Lettuce, soil moisture | No alert | Alert below 55%, Water Pump request and 50 L of water |
| Lettuce, humidity | No alert | Alert outside 65-85%, and a Humidifier or Dehumidifier request |

These were kept because sections 3.1 and 3.4 of the specification require them, and removing them would mean hardcoding crop names again. The 50 L follows the legacy Tomato rule, because the specification gives no figure for other crops.

### 4.4 Design decisions

- **Strategy pattern:** each alert type is its own `AlertRule` class, because the trigger logic differs: some check both limits, some only a minimum, and sensor failure checks validity. Adding an alert type means a new class, one `AlertType` value and one entry in `DefaultAlertRules`, with no change to existing rules. Rules with the same logic share it through `AlertChecks`.
- **Observer pattern:** `AlertListener` sends each alert to a list of `AlertHandler` classes, so a new reaction to alerts does not change existing ones.
- **Sensor failure alerts:** `SensorFailureRule` raises one for an invalid value such as `NaN`. Simulated sensors never produce one, so behaviour matches the legacy code in normal runs. A rule only runs when a reading arrives, so a sensor that sends nothing is not detected. An invalid reading is left out of report samples.
- **Light alerts are kept:** the legacy code creates them although the specification's list does not, so `AlertType` includes `LOW_LIGHT`.
- **Severity:** the legacy code has no severity levels, so the rules set `WARNING` or `CRITICAL` based on how far a reading is outside its limit.

## 5. Report Generation

### 5.1 Legacy design problems and fixes

| Method and lines | Problem | Implication | Fix applied |
| --- | --- | --- | --- |
| `generateDailyReport`, `saveArchiveReport` ([207-218](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L207-L218), [309-347](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L309-L347)) | Only one summary exists, built directly from global variables | The other reports in section 3.5 of the specification are missing | `ReportGenerator` interface with `StandardReportGenerator`, which supports all five report types |
| `totalWaterUsage`, `totalEnergyUsage` ([21-22](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L21-L22)) | Only running totals are kept | Usage cannot be broken down by chamber or time | `ReportSample` records each chamber's readings and usage per cycle |
| `processSensor`, `ChamberThread` ([141-205](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L141-L205), [291-292](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L291-L292)) | Usage amounts are scattered and tied to crop `if` chains | Amounts are hard to find or change | `UsageTracker` holds the rules, and `DefaultUsageRates` holds the per-crop amounts as data |

### 5.2 Concurrency

| Class | Issue in legacy | Fix applied |
| --- | --- | --- |
| `UsageTracker` | `+=` on shared counters ran with no lock, so additions were lost ([291-292](https://github.com/leeshel/SE301-Project/blob/532a86dc7f8ae14d4c19e463ebf1e26d0910fcbc/src/main/java/org/example/SVFRMS.java#L291-L292)) | A `synchronized` block protects the totals and per-chamber usage |
| `ReportSampleStore` | New class, shared by chambers | `synchronized` blocks around the list |
| `Report`, `ReportSample` | New classes | Immutable records, safe to share |
| `DailyReportService` | Legacy held `alertLock` and `reportLock` together | One `reportLock`, taken before the locks inside `AlertLog` and `UsageTracker` (lock ordering) |

### 5.3 Design decisions

- **Strategy pattern:** each report is its own `ReportStrategy` class, and `StandardReportGenerator` finds the right one by its `ReportType`. Adding a report means a new class, one enum value and one entry in `DefaultReportStrategies`, with no change to the generator or existing reports. Reports with the same logic share it through `ReportLines`.
- **Legacy report format is kept:** the report file has the same sections as before, followed by the new environmental report.
- **Nutrient usage is an assumption:** the legacy code has no figure, so each low-nutrient alert counts as 15%, the amount `NutrientInjector` adds.
- **Per-crop usage amounts are data:** Tomato and Strawberry have extra amounts in `DefaultUsageRates`, and other crops have none, matching the legacy code. The logic in `UsageTracker` does not name any crop.

## 6. Design Summary

### 6.1 Design patterns

| Pattern | Where | Problem it solves |
| --- | --- | --- |
| **Facade** | `FarmSystemFacade` | Callers can run the system without knowing its many classes |
| **Observer** | `ChamberMonitor` and `ReadingListener`, `AlertListener` and `AlertHandler` | One method no longer has to do every job, and a new job does not change existing ones |
| **Strategy** | `AlertRule`, `AutomatedDevice`, `ReportStrategy` | Rules can vary without changing the code that uses them |
| **Factory** | `SensorFactory` | Sensor creation and value ranges stay out of the monitoring code |
| **Builder** | `CropBuilder` | Crop profiles with optional ranges are readable and easy to extend |

### 6.2 SOLID principles

- **Single Responsibility:** the jobs of `processSensor` now sit in separate classes.
- **Open/Closed:** new crops, devices, alert rules, alert reactions and reports are added as new data or classes, without changing the code that uses them.
- **Liskov Substitution:** every device, sensor and listener can stand in for its interface. Crops are data rather than subclasses, which avoids inheritance problems.
- **Interface Segregation:** interfaces are small and focused, such as `Sensor`, `ReadingListener` and `AlertHandler`.
- **Dependency Inversion:** components depend on interfaces such as `CropRepository`, `AlertRule` and `ReportGenerator`. `FarmSystemFacade` and the `Default...` classes are where concrete classes are created.

### 6.3 Concurrency approach

| Tool | Where | Why |
| --- | --- | --- |
| `synchronized` blocks | `AlertLog`, `UsageTracker`, `ChamberState`, `ChamberSummary`, `ControlQueue`, `ReportSampleStore`, `SensorLogListener`, `DeviceController`, `DailyReportService` | Protect shared data, while slow work such as printing stays outside the lock for liveness |
| `ConcurrentHashMap` | `InMemoryCropRepository`, `ChamberStateStore`, `ChamberSummaryTracker`, `DeviceController` | Safe lookups and one-step inserts with `putIfAbsent` |
| Lock ordering | `DailyReportService` | `reportLock` is always taken first, and the classes it calls never call back, so a deadlock cycle cannot form |
| Fixed thread pool | `FarmSystemFacade` | The number of chambers is known, and each task runs for the whole simulation |
| Immutable objects | `CropProfile`, `Range`, `Alert`, `ReportSample`, `Report` | Threads can share them without locks |

**Tools not used:** `ReentrantReadWriteLock` helps when reads greatly outnumber writes, but chambers write on every reading and reads are rare. Lock timeouts are unnecessary because lock ordering already prevents the deadlock they would recover from.

**Allowed by design:** a sensor reading can overwrite a value just after a device changes it. No data is corrupted, the latest value wins, and the legacy code behaves the same way.

## 7. Evidence

### 7.1 Race conditions before and after

The same stress test ran on the refactored code before and after the concurrency fixes. Each "after" result held across repeated passes. "Before" values vary by run.

| Check | Before fixes | After fixes |
| --- | --- | --- |
| `AlertLog`, 8 threads, 400,000 alerts | List corrupted, and reading it threw `NullPointerException` | 400,000 generated and archived |
| `UsageTracker`, 8 threads, expected 3,200,000 L | 3,072,806 L | 3,200,000 L |
| Device read-then-write, 8 threads, expected 1,600,000 | 1,560,188 | 1,600,000 |
| Full application, 200 runs with 8 chambers | Wrong totals in all 200 runs | Wrong totals in 0 runs |
| `ChamberStateStore`, `ChamberSummaryTracker`, `ControlQueue` | Passed | Passed |

Passing stress tests is strong evidence, but it cannot prove that no race exists.

### 7.2 Other results

| Test | Result |
| --- | --- |
| Legacy application, 300 runs | 149 runs crashed a thread, caused by the `chamberStates` and `chamberSummaries` maps; none hung |
| Refactored application jar, 30 runs | No crashes or hangs |
| Same inputs to legacy and refactored code, 8,743 cases | 150 differ, all listed in section 4.3 |
| `SensorLogListener`, 8 threads writing 400,000 entries | All 400,000 kept |
| `InMemoryCropRepository`, 8 threads mixing saves, removes and reads | Finished without errors or hanging |
| New crop (Kale) added without editing existing logic | Alerts and all five reports include it |
| Invalid sensor reading (`NaN`) sent through the application | Sensor failure alert raised, and no crash |

The comparison test feeds readings one at a time on a single thread, so it checks logic rather than thread timing. The stress test covers thread timing.
