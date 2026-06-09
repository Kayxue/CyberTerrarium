# Terrarium MVC Integration

This package contains the Model and Controller side of the terrarium feature.
The Compose View lives in `src/jvmMain/kotlin/terrarium`; its top-level
`Terrarium` component is embedded by `page/Home.kt`. The View only consumes
`TerrariumSnapshot`.

## Package Layout

- `terrarium.model`
  - Data contracts for the terrarium domain.
  - Important classes:
    - `TerrariumSnapshot`: final state for the View.
    - `TerrariumEnvironmentState`: water/environment state after aggregation.
    - `TerrariumFishState`: fish state after aggregation.
    - `TerrariumSourceSnapshot`: one external resource's normalized output.
    - `TerrariumEnvironmentSignal`: environment impact from one source.
    - `TerrariumCreatureSignal`: raw creature/fish signal from one source.
- `terrarium.core`
  - Pure mapping and composition logic.
  - Important classes:
    - `TerrariumResourceAdapter`: read-only contract for external resources.
    - `JobTerrariumAdapter`: converts existing jobs into fish signals.
    - `SystemUsageTerrariumAdapter`: converts system metrics into environment signals.
    - `ProcessTerrariumAdapter`: converts the complete process tree into fish signals.
    - `TerrariumSnapshotComposer`: combines source snapshots into one terrarium snapshot.
- `terrarium.controller`
  - MVC Controller layer.
  - Important classes:
    - `ITerrariumController`
    - `TerrariumController`

There is no terrarium-owned repository layer. The job adapter may receive the
existing `IJobConfigRepository` to read priority, but never calls write methods.

## Main Contract

External resources should enter terrarium through `TerrariumResourceAdapter`.

```java
public interface TerrariumResourceAdapter {
    String getSourceId();

    String getDisplayName();

    TerrariumSourceSnapshot readSnapshot();
}
```

Rules:

- `readSnapshot()` must be read-only.
- Do not create, update, delete, run, or mutate external resources from an adapter.
- Convert external concepts into terrarium semantic values before returning.
- If a source is unavailable, return `TerrariumSourceSnapshot.unavailable(...)`.
- Do not throw for expected unavailable states. Throwing is reserved for unexpected failures; `TerrariumController` catches failures and converts them to unavailable source snapshots.

## Data Flow

```text
External source
  -> TerrariumResourceAdapter.readSnapshot()
  -> TerrariumSourceSnapshot
  -> TerrariumController.getSnapshot(...)
  -> TerrariumSnapshotComposer
  -> TerrariumSnapshot
  -> Compose View layer
```

The View renders only:

- `snapshot.getEnvironment()`
- `snapshot.getFish()`
- optionally `snapshot.getSources()` for source status/debug UI

The View should not call `JobController`, `SystemUsageSampler`, OSHI, or process
readers directly.

## Connecting Jobs

Jobs are already supported through `JobTerrariumAdapter`.

```java
IJobController jobController = JobController.createDefault();

TerrariumController terrariumController = new TerrariumController();
TerrariumSnapshot snapshot = terrariumController.getSnapshot(List.of(
    new JobTerrariumAdapter(jobController)
));
```

`JobTerrariumAdapter` currently reads:

- `IJobController.listJobs()`
- `IJobController.listFlowRuns()`
- `IJobController.listFlowRunJobs(...)`
- optionally `IJobConfigRepository.findOneById(...)` for display priority

It does not call mutating job APIs.

Mapping behavior:

- Every job becomes one `TerrariumCreatureSignal`.
- Successful latest result -> healthier, more active fish.
- Failed/cancelled/timeout latest result -> sick fish with high risk/stress.
- Disabled job -> inactive fish.
- No result yet -> unknown but moderately healthy fish.
- Job priority -> `visualHint.importance`, used by the 100-fish View limit.

The adapter then lets `TerrariumSnapshotComposer` apply environmental pressure
to the final `TerrariumFishState`.

## Connecting System Usage

System usage is supported through `SystemUsageTerrariumAdapter`, but this adapter
does not directly depend on the current `SystemUsageInfo` class. That is
intentional: `SystemUsageInfo` is in the default Java package, and named Java
packages cannot import default-package classes safely.

Use `TerrariumSystemMetrics` as the bridge value:

```java
TerrariumSystemMetrics metrics = new TerrariumSystemMetrics(
    cpuUsagePercent,
    cpuCurrentFrequencyHz,
    memoryUsedBytes,
    memoryTotalBytes,
    memoryUsagePercent,
    downloadBytesPerSecond,
    uploadBytesPerSecond,
    cpuTemperature
);

TerrariumSnapshot snapshot = new TerrariumController().getSnapshot(List.of(
    new SystemUsageTerrariumAdapter(metrics)
));
```

Expected usage from Kotlin/Compose later:

```kotlin
val usage = sampler.sampleLatest()
val metrics = TerrariumSystemMetrics(
    usage.cpuUsagePercent(),
    usage.cpuCurrentFrequencyHz(),
    usage.memoryUsedBytes(),
    usage.memoryTotalBytes(),
    usage.memoryUsagePercent(),
    usage.downloadBytesPerSecond(),
    usage.uploadBytesPerSecond(),
    usage.cpuTemperature()
)
```

Mapping behavior:

- High CPU adds stress and lowers environment health.
- High memory usage lowers clarity and health.
- High CPU temperature increases temperature stress and toxicity.
- Network throughput increases water motion.
- System usage does not emit fish; it emits environment signals.

## Connecting Processes

Processes are supported through `ProcessTerrariumAdapter`. It only calls the
read-only `ProcessManager.getProcessTrees()` method.

```java
ProcessManager processManager = new ProcessManager();

TerrariumSnapshot snapshot = new TerrariumController().getSnapshot(List.of(
    new ProcessTerrariumAdapter(processManager)
));
```

Reuse the same adapter or injected `ProcessManager` across snapshots because it
stores previous OSHI process ticks for later CPU usage calculations.

Every root and descendant process becomes a fish. PID controls identity and
position, process name/user controls color, resident memory controls size, and
CPU/state control activity, stress, risk, health, and motion. The adapter emits
no environment signals, avoiding duplicate system pressure calculations.

## Combining Sources

Normal usage combines all available sources:

```java
IJobController jobController = JobController.createDefault();
ProcessManager processManager = new ProcessManager();
TerrariumSystemMetrics metrics = SampledFromCurrentSystemUsage;

TerrariumSnapshot snapshot = new TerrariumController().getSnapshot(List.of(
    new SystemUsageTerrariumAdapter(metrics),
    new JobTerrariumAdapter(jobController),
    new ProcessTerrariumAdapter(processManager)
));
```

`TerrariumController` isolates adapter failures. If one adapter fails, the
controller creates an unavailable source snapshot for that adapter and still
returns a renderable `TerrariumSnapshot`.

## View Integration

The View layer does not duplicate mapping rules. It renders the model:

- Use `TerrariumEnvironmentState.health`, `clarity`, `stress`,
  `temperatureStress`, `waveIntensity`, `bubbleIntensity`, and `tint` to style
  the water.
- Use each `TerrariumFishState.health`, `stress`, `activity`, `risk`, `status`,
  and `visualHint` to style fish.
- Use `TerrariumSourceSnapshot.sourceStatus` and `message` to show source
  availability if needed.
- `state.rememberTerrariumSnapshot(...)` polls every two seconds by default and
  reuses the same `ProcessManager`.
- At most 100 fish are drawn, ordered by `visualHint.importance`, job kind,
  risk, and activity.
- Environment metrics, water tint, habitat colors, plants, and coral transition
  toward each new snapshot over two seconds instead of changing immediately.
- Fish keep their original trajectory and facing direction when snapshots
  update. New fish fade in; removed or replaced fish fade out over two seconds.
- Fish follow deterministic wide-area paths that span most of the tank. Each
  fish owns an independent horizontal position animation rather than deriving
  X from a repeating global phase. Fish travel to a boundary, remain there for
  700 ms, flip horizontally over 600 ms, then animate from that exact position
  toward the opposite boundary without wrapping.
- Hover shows compact information; clicking opens the fish details dialog.

If the View needs a new visual cue, prefer adding a semantic field to model/core
only when the existing values are not enough. Avoid making the View inspect
external job/process/system classes directly.

## Current Limits

- Terrarium has no DB repository because it does not persist data.
- Jobs are read through existing job controller APIs only.
- System usage must be bridged through `TerrariumSystemMetrics`.
- Processes are flattened; parent/child relationships are not visualized.
- Lower-ranked fish remain in the snapshot when the View draws its top 100.
