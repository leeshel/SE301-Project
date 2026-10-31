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

### `src/main/java/org/example/device`

```text
device/
├── AutomatedDevice.java   # Common interface for automated devices.
├── DeviceController.java  # Registers and manages device activation.
├── CoolingFan.java        # Reduces chamber temperature.
├── WaterPump.java         # Increases soil moisture.
├── NutrientInjector.java  # Increases nutrient level.
├── Humidifier.java        # Increases chamber humidity.
├── Dehumidifier.java      # Reduces chamber humidity.
├── GrowLight.java         # Increases light intensity.
├── HeatingUnit.java       # Increases chamber temperature.
├── VentilationSystem.java # Reduces chamber humidity.
└── ShadeController.java   # Reduces light intensity.
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

### Automated Device Control

The original device activation logic used a long conditional structure in
`SVFRMS.activateControl()` to identify and execute individual device operations.
This was refactored into a modular device-control architecture:

```text
Control Request
        ↓
SVFRMS.activateControl()
        ↓
DeviceController
        ↓ Device lookup by name
AutomatedDevice
        ↓
Individual Device Implementation
        ↓
Update ChamberState
        ↓
Return activation result
        ↓
SVFRMS.handleEvent()
```

`AutomatedDevice` defines a common interface for all automated devices, while
each device class implements its own activation behaviour. `DeviceController`
maintains a `HashMap` registry of device implementations, allowing devices to
be retrieved and activated by name instead of relying on multiple conditional
branches.

The device component consists of the following implementations:

- `CoolingFan`: Reduces chamber temperature by 5°C, with a minimum of 18°C.
- `WaterPump`: Increases soil moisture by 20%, up to a maximum of 100%.
- `NutrientInjector`: Increases nutrient level by 15%, up to a maximum of 100%.
- `Humidifier`: Increases humidity by 10%, up to a maximum of 100%.
- `Dehumidifier`: Reduces humidity by 10%, with a minimum of 30%.
- `GrowLight`: Increases light intensity by 150 lux, up to a maximum of 1000 lux.
- `HeatingUnit`: Increases chamber temperature.
- `VentilationSystem`: Reduces chamber humidity.
- `ShadeController`: Reduces light intensity.

The first six devices preserve their original environmental adjustment values
and limits. The Heating Unit, Ventilation System, and Shade Controller extend
the available device-control capabilities.

Device-specific environmental updates are encapsulated within their respective
classes, while `SVFRMS` retains responsibility for event tracking and control
logging through `handleEvent()`.

### SOLID principles

- **Single Responsibility:** Alert evaluation, observation data, report data,
  and report generation are separated into different classes. Each automated
  device also handles its own environmental control operation, while
  `DeviceController` manages device registration and activation.
- **Open/Closed:** New alert evaluators and report-generation strategies can be
  added without changing their callers. Similarly, new automated devices can
  implement `AutomatedDevice` and be registered without adding further
  conditional branches to the device activation logic.
- **Interface Segregation:** Small `AlertEvaluator` and `ReportGenerator`
  interfaces avoid forcing classes to implement unrelated operations.
  `AutomatedDevice` provides a focused interface for device activation.
- **Dependency Inversion:** Callers depend on `AlertEvaluator` and
  `ReportGenerator` rather than concrete implementations. `EnvironmentalObservation`
  uses the existing `SensorType` values to identify readings supplied by sensors.
  `DeviceController` also interacts with devices through the `AutomatedDevice`
  abstraction rather than depending on individual device implementations.

### GoF design patterns

- **Strategy:** `StandardReportGenerator` selects report-generation behavior by
  `ReportType`; `AlertEvaluator` allows alternative alert policies.
  `AutomatedDevice` applies the same principle to device control, where each
  device provides its own implementation of the activation operation and
  `DeviceController` selects the appropriate implementation at runtime.
- **Builder:** The existing crop code uses `CropBuilder` to construct profiles
  with optional environmental ranges used by alert evaluation.

### Race conditions

- `EnvironmentalObservation`, `Alert`, `Report`, and `ReportSample` are
  immutable, so they can be safely shared between threads.
- The report generator and alert evaluator do not maintain shared mutable
  counters or collections, reducing race-condition risk.
- `DeviceController` uses a `HashMap` to maintain its device registry, which is
  populated during initialisation and used for device lookup. The registry is
  not designed for concurrent modification.
- Device activation modifies the shared mutable `ChamberState` environmental
  values. Concurrent access by sensor and control threads still requires
  appropriate synchronisation to avoid race conditions.

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
- The device controller does not introduce additional locks. Device activation
  continues through the existing control-request and `ControlThread` workflow,
  preserving the existing request-processing mechanism. However, synchronisation
  of shared `ChamberState` values remains necessary when accessed concurrently
  by sensor and control threads.
  