# Terrarium MVC Integration

This package contains the Model and Controller side of the terrarium feature.
It does not render UI yet. The future View layer should only consume
`TerrariumSnapshot` and should not know whether data came from jobs, system
usage, processes, or another source.

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
    - `UnavailableProcessTerrariumAdapter`: explicit placeholder until process reading exists.
    - `TerrariumSnapshotComposer`: combines source snapshots into one terrarium snapshot.
- `terrarium.controller`
  - MVC Controller layer.
  - Important classes:
    - `ITerrariumController`
    - `TerrariumController`

There is no `repository` layer for terrarium right now because terrarium does
not own persistence and does not read/write DB directly. It consumes external
read-only data through adapters.

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
  -> future View layer
```

The future View should render only:

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

It does not call mutating job APIs.

Mapping behavior:

- Every job becomes one `TerrariumCreatureSignal`.
- Successful latest result -> healthier, more active fish.
- Failed/cancelled/timeout latest result -> sick fish with high risk/stress.
- Disabled job -> inactive fish.
- No result yet -> unknown but moderately healthy fish.

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

Process reading is not implemented yet. Use the explicit placeholder for now:

```java
TerrariumSnapshot snapshot = new TerrariumController().getSnapshot(List.of(
    new UnavailableProcessTerrariumAdapter()
));
```

This source returns:

- `sourceStatus = UNAVAILABLE`
- no environment signals
- no fish
- a message explaining that process reading is not implemented

When process reading exists, replace `UnavailableProcessTerrariumAdapter` with a
real process adapter:

```java
public final class ProcessTerrariumAdapter implements TerrariumResourceAdapter {
    private final ProcessReader reader;

    public ProcessTerrariumAdapter(ProcessReader reader) {
        this.reader = reader;
    }

    @Override
    public String getSourceId() {
        return "processes";
    }

    @Override
    public String getDisplayName() {
        return "Processes";
    }

    @Override
    public TerrariumSourceSnapshot readSnapshot() {
        List<TerrariumCreatureSignal> fish = new ArrayList<>();

        for (ProcessInfo process : reader.listProcesses()) {
            fish.add(new TerrariumCreatureSignal(
                "process:" + process.getPid(),
                process.getName(),
                TerrariumCreatureKind.PROCESS,
                getSourceId(),
                String.valueOf(process.getPid()),
                processHealth(process),
                processStress(process),
                processActivity(process),
                processRisk(process),
                processStatus(process),
                TerrariumVisualHint.stable(
                    String.valueOf(process.getPid()),
                    processSize(process),
                    processMotion(process)
                )
            ));
        }

        return new TerrariumSourceSnapshot(
            getSourceId(),
            getDisplayName(),
            TerrariumSourceStatus.AVAILABLE,
            List.of(),
            fish,
            Instant.now(),
            ""
        );
    }
}
```

The process reader itself should live outside terrarium if it belongs to another
module. The terrarium adapter should only translate read-only process data into
terrarium semantic values.

## Combining Sources

The normal future usage should combine all available sources:

```java
IJobController jobController = JobController.createDefault();
TerrariumSystemMetrics metrics = SampledFromCurrentSystemUsage;

TerrariumSnapshot snapshot = new TerrariumController().getSnapshot(List.of(
    new SystemUsageTerrariumAdapter(metrics),
    new JobTerrariumAdapter(jobController),
    new UnavailableProcessTerrariumAdapter()
));
```

`TerrariumController` isolates adapter failures. If one adapter fails, the
controller creates an unavailable source snapshot for that adapter and still
returns a renderable `TerrariumSnapshot`.

## What The View Should Do Later

The View layer should not duplicate mapping rules. It should render the model:

- Use `TerrariumEnvironmentState.health`, `clarity`, `stress`,
  `temperatureStress`, `waveIntensity`, `bubbleIntensity`, and `tint` to style
  the water.
- Use each `TerrariumFishState.health`, `stress`, `activity`, `risk`, `status`,
  and `visualHint` to style fish.
- Use `TerrariumSourceSnapshot.sourceStatus` and `message` to show source
  availability if needed.

If the View needs a new visual cue, prefer adding a semantic field to model/core
only when the existing values are not enough. Avoid making the View inspect
external job/process/system classes directly.

## Current Limits

- Terrarium has no DB repository because it does not persist data.
- Jobs are read through existing job controller APIs only.
- System usage must be bridged through `TerrariumSystemMetrics`.
- Process support is intentionally unavailable until a real process reader
  exists.
- No View is implemented in this package yet.
