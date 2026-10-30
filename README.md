# SE301-Project

## Directory Layout

### `src/main/java/org/example/report`

```text
report/
├── Report.java                  # Immutable completed report with text output.
├── ReportGenerator.java         # Interface for generating reports.
├── ReportSample.java            # Validated environmental and resource-use data.
├── ReportType.java              # Enumerates the five supported report types.
└── StandardReportGenerator.java # Generates environmental, crop, and usage reports.
```

### `src/main/java/org/example/sensor`

```text
sensor/
├── ChamberMonitor.java          # Runs sensor cycles and publishes readings.
├── ChamberState.java            # Thread-safe sensor state for one chamber.
├── ChamberStateListener.java    # Listener contract for chamber-state changes.
├── ChamberStateStore.java       # Concurrent store of chamber states.
├── ReadingListener.java         # Listener contract for sensor readings.
├── Sensor.java                  # Interface for sensor implementations.
├── SensorFactory.java           # Creates the sensors used by a chamber.
├── SensorLogListener.java       # Thread-safe listener for recording readings.
├── SensorReading.java           # Immutable sensor reading value.
├── SensorType.java              # Enumerates supported sensor types.
└── SimulatedSensor.java         # Generates simulated sensor values.
```

## Design and Concurrency Notes

### SOLID principles

- **Single Responsibility:** Sensor reading, chamber state, logging, report data,
  and report generation are separated into different classes.
- **Open/Closed:** New `Sensor` implementations and report-generation
  strategies can be added without changing the monitoring workflow.
- **Liskov Substitution:** `SimulatedSensor` can be used wherever the `Sensor`
  interface is expected.
- **Interface Segregation:** Small `Sensor`, `ReadingListener`, and
  `ReportGenerator` interfaces avoid forcing classes to implement unrelated
  operations.
- **Dependency Inversion:** `ChamberMonitor` depends on sensor and listener
  interfaces, while callers depend on `ReportGenerator` rather than the
  concrete report generator.

### GoF design patterns

- **Factory:** `SensorFactory` centralizes creation of chamber sensors.
- **Observer:** `ChamberMonitor` publishes readings to `ReadingListener`
  subscribers without knowing their concrete implementations.
- **Strategy:** `StandardReportGenerator` selects report-generation behavior by
  `ReportType`, and `ReportGenerator` allows alternative implementations.
- **Builder:** The crop package uses `CropBuilder` to construct profiles with
  optional environmental ranges.

### Race conditions

- `ChamberStateStore` uses `ConcurrentHashMap.computeIfAbsent`, making chamber
  state creation atomic.
- `ChamberState` synchronizes access to its mutable sensor values.
- `SensorLogListener` protects shared log updates.
- `Report`, `ReportSample`, `SensorReading`, and crop value objects are immutable,
  so they can be safely shared between threads.
- The report generator and alert evaluator do not maintain shared mutable
  counters or collections, reducing race-condition risk.

### Deadlocks

- The standalone report and alert components use no locks, so they introduce no
  deadlock path.
- The sensor components use short, method-level synchronization and do not
  acquire multiple locks in conflicting orders.
- Future integrations should avoid nested locks and should maintain one
  consistent lock-acquisition order.
- These claims apply to the refactored components documented here; the legacy
  `SVFRMS` execution path must still be integrated and tested before the whole
  application can be declared free of race conditions and deadlocks.
