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
- These claims apply to the refactored components documented here; the legacy
  `SVFRMS` execution path must still be integrated and tested before the whole
  application can be declared free of race conditions and deadlocks.
