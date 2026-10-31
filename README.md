# SE301-Project

## Directory Layout

### `src/main/java/org/example/alert`

```text
alert/
├── Alert.java                    # Immutable alert result.
├── AlertEvaluator.java           # Interface for evaluating observations.
├── AlertSeverity.java            # Defines INFO, WARNING, and CRITICAL levels.
├── AlertType.java                # Enumerates supported alert categories.
├── EnvironmentalObservation.java # Validated readings for one chamber.
├── LegacyCompatibleAlertEvaluator.java # Preserves legacy alert thresholds.
├── ReadingAlertEvaluator.java    # Interface for reading-level alert policies.
└── StandardAlertEvaluator.java   # Applies the standard alert rules.
```

### `src/main/java/org/example/report`

```text
report/
├── Report.java                  # Immutable completed report with text output.
├── ReportGenerator.java         # Interface for generating reports.
├── ReportSample.java            # Validated environmental and resource-use data.
├── ReportType.java              # Enumerates the five supported report types.
└── StandardReportGenerator.java # Generates environmental, crop, and usage reports.
```

## Design and Concurrency Notes

### Integrated flow

The legacy entry point now uses the modernised components without changing its
legacy alert messages, control requests, sensor ranges, or resource increments:

```text
SensorFactory/ChamberMonitor
        ↓ SensorReading
SVFRMS processSensor
        ↓ CropRepository/CropProfile
LegacyCompatibleAlertEvaluator
        ↓ Alert
legacy alert, control, and summary handling
        ↓ ReportSample
StandardReportGenerator
        ↓
archive/daily_report.txt
```

`LegacyCompatibleAlertEvaluator` deliberately keeps the original thresholds:
nutrient alerts below 40%, light alerts below 500 lux, and the original
crop-specific temperature, humidity, and soil-moisture rules. The generic
`StandardAlertEvaluator` remains available for the standalone specification
policy.

### SOLID principles

- **Single Responsibility:** Alert evaluation, observation data, report data,
  and report generation are separated into different classes.
- **Open/Closed:** New alert evaluators and report-generation strategies can be
  added without changing their callers.
- **Interface Segregation:** Small `AlertEvaluator` and `ReportGenerator`
  interfaces avoid forcing classes to implement unrelated operations.
- **Dependency Inversion:** Callers depend on `AlertEvaluator` and
  `ReportGenerator` rather than concrete implementations. `EnvironmentalObservation`
  uses the existing `SensorType` values to identify readings supplied by sensors.

### GoF design patterns

- **Strategy:** `StandardReportGenerator` selects report-generation behavior by
  `ReportType`; `AlertEvaluator` allows alternative alert policies.
- **Builder:** The existing crop code uses `CropBuilder` to construct profiles
  with optional environmental ranges used by alert evaluation.

### Race conditions

- `EnvironmentalObservation`, `Alert`, `Report`, and `ReportSample` are
  immutable, so they can be safely shared between threads.
- The report generator and alert evaluator do not maintain shared mutable
  counters or collections, reducing race-condition risk.

### Deadlocks

- The standalone report and alert components use no locks, so they introduce no
  deadlock path.
- Future integrations should avoid nested locks and should maintain one
  consistent lock-acquisition order.
- The integrated report sample collection is synchronized, and the modern
  sensor monitor publishes readings without shared mutable state of its own.
- The legacy global counters and maps remain part of the compatibility path;
  they require a separate concurrency refactor before the whole application can
  be declared free of race conditions.
