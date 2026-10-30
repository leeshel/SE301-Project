# SE301-Project

## Refactoring Notes

These notes record the problems found in the legacy code and the fixes applied so far. Line numbers refer to [SVFRMS.java](src/main/java/org/example/SVFRMS.java), which stays unchanged as the team's reference.

**Current status:** the new classes are in `org.example.crop` and `org.example.sensor`, but they are not connected to the running app yet. Running the app still runs the legacy `SVFRMS`, so its problems remain until the refactor is complete.

## 1. Crop Profile Management

### 1.1 Legacy problems and fixes

| Method and lines                                                                                                                                                                                    | Problem                                                                                                                                              | Implication                                                                                                                | Fix applied                                                                                                                                               |
| --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `processSensor` ([147-170](src/main/java/org/example/SVFRMS.java#L147-L170), [174-181](src/main/java/org/example/SVFRMS.java#L174-L181), [185-187](src/main/java/org/example/SVFRMS.java#L185-L187)) | Crop thresholds are hardcoded in `if/else` chains                                                                                                  | Adding or removing a crop means editing this method, which breaks the open/closed principle and risks breaking other crops | Thresholds move into `CropProfile` objects, created with `CropBuilder`. Adding a crop is one `save(...)` call                                       |
| `processSensor` (same lines)                                                                                                                                                                      | Crop names are repeated strings, such as `crop.equals("Tomato")`                                                                                   | A misspelt or unknown crop matches no branch, so its checks are skipped with no error                                      | `CropRepository.findByName` returns an empty result for an unknown crop, so it can be detected                                                          |
| `processSensor` (same lines)                                                                                                                                                                      | Threshold numbers are mixed into the logic and copied into each crop's branch                                                                        | Values are hard to find, compare or change, and there is no check that a minimum is below its maximum                      | `DefaultCropProfiles` holds the starting data in one place, and `Range` rejects `min > max`                                                         |
| `processSensor` ([174-187](src/main/java/org/example/SVFRMS.java#L174-L187), [191](src/main/java/org/example/SVFRMS.java#L191))                                                                     | Crop data is incomplete: humidity covers only Tomato and Strawberry, soil moisture only Tomato, and light is one fixed 500 lux minimum for all crops | The code does not store the ranges required by section 3.1 of the specification                                            | `CropProfile` has all four ranges. Spinach, Chili and Cucumber leave humidity and soil moisture empty because no source defines them                    |
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
- **Light range is inferred:** the specification gives no light values, so 500-1000 lux comes from the legacy 500 lux alert threshold and the Grow Light's 1000 lux cap ([line 124](src/main/java/org/example/SVFRMS.java#L124)).

### 1.4 Still to do

Alert logic does not read these profiles yet. To preserve existing behaviour, the Alert step must account for these differences between the specification and the legacy code:

- The specification gives Lettuce a humidity range, and Lettuce and Strawberry soil moisture ranges, but the legacy code never alerts on them.
- The legacy code has no alert for soil moisture or light above a maximum.
- The legacy code adds different water and energy amounts per crop. These are not profile attributes in the specification, so they need a home elsewhere.

## 2. Environment Sensors

### 2.1 Legacy design problems and fixes

| Method and lines                                                                                                                                                                                                                                                             | Problem                                                                                            | Implication                                                                                                             | Fix applied                                                                                                                                                                                                         |
| ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `processSensor` ([141-205](src/main/java/org/example/SVFRMS.java#L141-L205))                                                                                                                                                                                                | One method does logging, state updates, threshold checks, alerts, usage totals and device requests | It breaks the single responsibility principle, so any change risks the other jobs, and they cannot be tested separately | **Observer pattern:** `ChamberMonitor` publishes each `SensorReading` to `ReadingListener` objects, with one listener per job. Logging and state updates have moved; the other jobs move in later steps |
| `processSensor` ([145](src/main/java/org/example/SVFRMS.java#L145), [172](src/main/java/org/example/SVFRMS.java#L172), [183](src/main/java/org/example/SVFRMS.java#L183), [189](src/main/java/org/example/SVFRMS.java#L189), [197](src/main/java/org/example/SVFRMS.java#L197)) | Sensor types are strings compared in an `if/else` chain                                          | A misspelt type is silently ignored, and a new sensor means editing the chain                                           | `SensorType` enum, so the compiler catches invalid types                                                                                                                                                          |
| `ChamberThread.run` ([285-293](src/main/java/org/example/SVFRMS.java#L285-L293))                                                                                                                                                                                            | Sensor value ranges are hardcoded inside the thread loop                                           | Sensors cannot be replaced or configured, and real hardware would mean rewriting the loop                               | `Sensor` interface, `SimulatedSensor` implementation, and **Factory pattern** through `SensorFactory`                                                                                                   |
| `ChamberThread` ([275](src/main/java/org/example/SVFRMS.java#L275))                                                                                                                                                                                                         | The class extends `Thread`, mixing the work with how it runs                                     | The monitoring work is tied to one way of running it                                                                    | `ChamberMonitor` implements `Runnable` and depends only on the `Sensor` and `ReadingListener` interfaces                                                                                                    |
| `random` ([line 31](src/main/java/org/example/SVFRMS.java#L31))                                                                                                                                                                                                             | All chambers share one static `Random`. This is thread-safe, so it is not a race condition       | Threads compete for it, and tests cannot control its values                                                             | `SensorFactory` takes a `Random` from the caller, so each chamber can have its own and tests can use a fixed seed                                                                                               |

### 2.2 Race conditions and fixes

| Method and lines                                                                                                                                                                                                                                                                                                                                              | Race condition                                                                                            | Implication                                                                                                                                      | Fix applied                                                                                            | Status                                                                               |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------ |
| `getChamberState` ([48-53](src/main/java/org/example/SVFRMS.java#L48-L53))                                                                                                                                                                                                                                                                                   | Check-then-insert on a shared `HashMap` with no lock, so an insert can be lost and `get` returns null | `NullPointerException` kills the chamber thread, for example at [line 146](src/main/java/org/example/SVFRMS.java#L146), and it stops processing | `ChamberStateStore` uses `ConcurrentHashMap.computeIfAbsent`, which checks and inserts as one step | Fixed                                                                                |
| `processSensor` ([146](src/main/java/org/example/SVFRMS.java#L146), [173](src/main/java/org/example/SVFRMS.java#L173), [184](src/main/java/org/example/SVFRMS.java#L184), [190](src/main/java/org/example/SVFRMS.java#L190), [198](src/main/java/org/example/SVFRMS.java#L198)) vs `activateControl` ([94-124](src/main/java/org/example/SVFRMS.java#L94-L124)) | Chamber threads overwrite values while the control thread reads and changes them, with no lock            | Sensor updates or device adjustments can be lost, leaving wrong chamber values                                                                   | `ChamberState` uses synchronized methods                                                             | Partly fixed: device code must wrap each read-then-write in `synchronized (state)` |
| `handleEvent` ([line 64](src/main/java/org/example/SVFRMS.java#L64))                                                                                                                                                                                                                                                                                         | Chamber threads add to a shared `ArrayList` with no lock                                                | Log entries can be lost                                                                                                                          | `SensorLogListener` uses a synchronized block, with the text built outside it for liveness           | Fixed                                                                                |

### 2.3 Deadlock

- **Legacy:** this section has no deadlock. `processSensor` can block in `addControlRequest` when the control queue is full ([133-135](src/main/java/org/example/SVFRMS.java#L133-L135)), which would become a permanent hang if the control thread died. The queue moves in the Device step.
- **Refactored:** no deadlock is possible in this package today. Only `ChamberState` and `SensorLogListener` take locks, no code holds two locks at once, and locked sections never call listeners or other outside code.
- **Later steps:** if the Alert and Report steps need two locks together, they must always take them in the same order.

### 2.4 Design decisions

- **One `SimulatedSensor` class:** legacy sensors differ only in their value range, so five subclasses would repeat the same code.
- **Small interfaces:** `Sensor` and `ReadingListener` each have one focused purpose, instead of one large `EnvironmentSensors` interface (interface segregation).
- **Listener order matters:** the entry point must register logging first, then state, then alerts and device control, to match the legacy order.
- **Listeners must be thread-safe:** the chamber threads can share one listener, so it may be called from several threads at once.

### 2.5 Still to do

- **Usage totals:** the per-cycle `totalWaterUsage += 2` and `totalEnergyUsage += 1` ([291-292](src/main/java/org/example/SVFRMS.java#L291-L292)) have not moved. Totals will be too low until they do.
- **Other `processSensor` jobs:** threshold checks, alerts, usage totals and device requests remain to be moved.
- **`chamberSummaries`:** `getChamberSummary` ([55-60](src/main/java/org/example/SVFRMS.java#L55-L60)) has the same race as `getChamberState` and is not fixed yet.

## 3. Evidence

| Test                                                                  | Result                                                                                              |
| --------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| Legacy app, 300 runs                                                  | 149 runs crashed a thread, caused by the `chamberStates` and `chamberSummaries` maps; none hung |
| `ChamberStateStore`, 2,000 rounds with 8 threads                    | Always one state per chamber                                                                        |
| `SensorLogListener`, 8 threads writing 400,000 entries              | All 400,000 kept                                                                                    |
| `InMemoryCropRepository`, 8 threads mixing saves, removes and reads | Finished without errors or hanging                                                                  |

These tests are separate and do not compare the same program. A before-and-after comparison needs a rerun once the app runs on the refactored code.
