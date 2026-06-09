# Terrarium 核心運作流程

Terrarium 的核心流程可以簡化成：

```text
外部原始資料
  -> Adapter 翻譯成水族缸語意
  -> Controller 收集各來源結果
  -> Composer 合併與計算
  -> TerrariumSnapshot
  -> Compose View 繪製水、環境與魚
```

Java `terrarium` package 實作 MVC 的 Model 與 Controller；Compose View 位於
`src/jvmMain/kotlin/terrarium`，並由 `page/Home.kt` 嵌入頂層 `Terrarium`
component。

## Snapshot 是什麼？

Snapshot 是「某一個時間點的完整狀態快照」。

`TerrariumSnapshot` 不是會自行持續更新的物件，也不是資料庫紀錄。它代表呼叫
`TerrariumController.getSnapshot(...)` 當下，整個水族缸應該呈現的狀態。

`TerrariumSnapshot` 包含：

```java
TerrariumEnvironmentState environment; // 水族缸環境
List<TerrariumFishState> fish;          // 所有魚
List<TerrariumSourceSnapshot> sources;  // 各資料來源狀態
Instant sampledAt;                      // 快照產生時間
```

例如某次 snapshot 可能代表：

- 環境健康度為 42。
- 水質清澈度為 55。
- 波浪強度為 0.7。
- 有 5 隻健康的魚與 2 隻生病的魚。
- System usage 成功取得。
- Jobs 成功取得。
- Processes 成功取得。

可以把 `TerrariumSnapshot` 理解成水族缸的一張照片。定期取得新的 snapshot，
才會形成看起來持續更新的動態水族缸。

## SourceSnapshot 與 TerrariumSnapshot

`TerrariumSourceSnapshot` 是「單一外部來源」產生的結果。

例如：

```text
System usage -> 一個 TerrariumSourceSnapshot
Jobs         -> 一個 TerrariumSourceSnapshot
Processes    -> 一個 TerrariumSourceSnapshot
```

每個 adapter 先把自己的外部資料轉成 `TerrariumSourceSnapshot`，再由 Composer
合併成一個最終的 `TerrariumSnapshot`：

```text
多個 TerrariumSourceSnapshot
             |
             v
TerrariumSnapshotComposer
             |
             v
一個 TerrariumSnapshot
```

`TerrariumSourceSnapshot` 包含：

- `sourceId`：來源的穩定識別碼。
- `displayName`：來源的顯示名稱。
- `sourceStatus`：`AVAILABLE`、`DEGRADED` 或 `UNAVAILABLE`。
- `environmentSignals`：該來源對整體環境的影響。
- `creatureSignals`：該來源所代表的生物/魚。
- `sampledAt`：來源資料取得時間。
- `message`：不可用、降級或錯誤說明。

## 完整處理流程

### 1. 準備 Resource Adapters

呼叫端先準備所有要接入水族缸的 adapters：

```java
List<TerrariumResourceAdapter> adapters = List.of(
    new SystemUsageTerrariumAdapter(metrics),
    new JobTerrariumAdapter(jobController),
    new ProcessTerrariumAdapter(processManager)
);
```

`TerrariumResourceAdapter` 是外部資源與 terrarium 之間的 read-only contract：

```java
public interface TerrariumResourceAdapter {
    String getSourceId();

    String getDisplayName();

    TerrariumSourceSnapshot readSnapshot();
}
```

Adapter 的責任是把完全不同的外部資料，翻譯成統一的水族缸語意。

Adapter 不可以：

- 建立外部資源。
- 修改外部資源。
- 刪除外部資源。
- 執行 job 或 process。
- 寫入外部資料庫。

### 2. 呼叫 TerrariumController

呼叫：

```java
TerrariumSnapshot snapshot =
    new TerrariumController().getSnapshot(adapters);
```

`TerrariumController` 會依序呼叫每個 adapter：

```java
adapter.readSnapshot();
```

每個 adapter 都會回傳自己的 `TerrariumSourceSnapshot`。

如果某個 adapter 發生非預期錯誤，Controller 不會讓整個水族缸失敗，而會把該
來源轉成：

```text
sourceStatus = UNAVAILABLE
message = 錯誤原因
```

其他正常來源仍然可以繼續組成最後的 snapshot。

### 3. System Usage 轉成環境訊號

System usage 的資料流：

```text
SystemUsageSampler
  -> SystemUsageInfo
  -> TerrariumSystemMetrics
  -> SystemUsageTerrariumAdapter
  -> TerrariumEnvironmentSignal
```

`SystemUsageInfo` 位於 Java default package，因此具名 package 的 Java 類別不能
直接安全 import。`TerrariumSystemMetrics` 是 system usage 與 terrarium 之間的
橋接 Model。

`SystemUsageTerrariumAdapter` 目前會把資料轉換為：

- CPU 高：降低環境 health，增加 stress。
- Memory usage 高：降低 clarity 與 health。
- CPU temperature 高：增加 temperature stress 與 toxicity。
- Network throughput 高：增加 water motion。

System usage 目前不產生魚，只產生 `TerrariumEnvironmentSignal`。

### 4. Jobs 轉成魚訊號

`JobTerrariumAdapter` 透過既有 `IJobController` 讀取：

```java
listJobs();
listFlowRuns();
listFlowRunJobs(...);
```

若注入既有 `IJobConfigRepository`，adapter 只呼叫 `findOneById(...)` 讀取
priority。Terrarium 不呼叫 repository 的 save/update/delete，也不修改外部
job package。

每個 job 會產生一個 `TerrariumCreatureSignal`：

- `SUCCESS`：健康度與活動度較高。
- `FAILED`：生病、壓力與風險較高。
- `TIMEOUT`：健康度最低，風險最高。
- `RUNNING`：活動度高，壓力偏高。
- Disabled job：狀態為 `INACTIVE`。
- 尚無執行結果的啟用 Job：視為 `IDLE`，狀態為 `HEALTHY`。
- Job priority：轉成 `visualHint.importance`，供 View 選取最多 100 隻魚。

目前 job 執行架構主要在 job 結束後保存結果，因此 terrarium 現階段主要呈現
最近一次執行結果。即時 `RUNNING` 狀態必須等外部 job 模組提供可讀取的即時狀態
後才能完整呈現。

### 5. Processes 轉成魚訊號

`ProcessTerrariumAdapter` 只透過既有 `ProcessManager` 的 read-only API：

```java
processManager.getProcessTrees();
```

它不會呼叫 process termination API。Adapter 會遞迴走訪所有 root 與 children，
並把每個 process 轉成 `TerrariumCreatureSignal(PROCESS)`：

- PID 作為 `id`、`sourceRef` 與位置 seed。
- Name/user 產生顏色 seed。
- CPU usage 影響 activity、stress 與 importance。
- Resident memory 只影響魚的大小。
- `STOPPED` 成為 `INACTIVE`。
- `ZOMBIE`、`INVALID` 成為 `SICK` 並提高 risk。

Process adapter 不產生 environment signals，避免與 system usage 重複計算。
同一個 `ProcessManager` 必須跨 snapshots 重用，讓後續 CPU usage 可使用前一次
OSHI ticks。Process hierarchy 目前會攤平成 fish list。

### 6. Composer 合併環境

Controller 收集完各來源後，會把所有 `TerrariumSourceSnapshot` 交給
`TerrariumSnapshotComposer`。

Composer 會累加所有 `TerrariumEnvironmentSignal`：

```text
health      = 100 + 所有 healthImpact
stress      = 所有 stressImpact
clarity     = 100 + clarityImpact - toxicity / 2
temperature = 所有正向 temperatureImpact
motion      = 基礎 motion + 所有 motionImpact
toxicity    = 所有正向 toxicityImpact
```

所有百分比類數值最後都會被限制在 `0..100`。

接著 Composer 計算：

- `waveIntensity`
- `bubbleIntensity`
- `TerrariumWaterTint`

水色選擇規則：

- 環境健康或 toxicity 很差：`sick` tint。
- 環境壓力偏高：`stressed` tint。
- 其他情況：`healthy` tint。

最後得到 `TerrariumEnvironmentState`。

### 7. Composer 計算最終魚狀態

Adapter 先產生 `TerrariumCreatureSignal`，Composer 再將它轉成最終的
`TerrariumFishState`。

魚的最終健康度不只看魚本身，也會受整體環境影響：

```text
最終 health =
    creature signal health * 75%
    + environment health * 25%
```

最終壓力：

```text
最終 stress =
    creature signal stress * 80%
    + environment 惡化程度 * 20%
```

因此，即使某個 job 最近執行成功，如果 CPU、memory、temperature 都很糟，
代表這隻魚仍生活在不健康的環境中，最終健康度也會受到影響。

狀態還會進一步調整：

- 原本是 `HEALTHY`，但最終 health 低於 55，改成 `STRESSED`。
- 最終 health 低於 35，且不是 `INACTIVE`，改成 `SICK`。

### 8. 產生最終 TerrariumSnapshot

Composer 最後建立：

```java
new TerrariumSnapshot(
    environment,
    fish,
    sourceSnapshots,
    Instant.now()
);
```

View 只需要讀取：

```java
snapshot.getEnvironment();
snapshot.getFish();
snapshot.getSources();
snapshot.getSampledAt();
```

View 不需要知道 CPU、`JobStatus`、OSHI 或 process reader 的具體類別。

## 一個完整的呼叫範例

```java
IJobController jobController = JobController.createDefault();
ProcessManager processManager = new ProcessManager();

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

List<TerrariumResourceAdapter> adapters = List.of(
    new SystemUsageTerrariumAdapter(metrics),
    new JobTerrariumAdapter(jobController),
    new ProcessTerrariumAdapter(processManager)
);

TerrariumController controller = new TerrariumController();
TerrariumSnapshot snapshot = controller.getSnapshot(adapters);
```

產生後：

```java
TerrariumEnvironmentState environment = snapshot.getEnvironment();
List<TerrariumFishState> fish = snapshot.getFish();
List<TerrariumSourceSnapshot> sources = snapshot.getSources();
```

## Snapshot 如何持續更新

Controller 本身的行為是：

```text
呼叫一次 getSnapshot()
  -> 讀取一次所有 adapters
  -> 產生一次 TerrariumSnapshot
```

`TerrariumSnapshot` 本身不會自動變化。

目前 `state.rememberTerrariumSnapshot(...)` 預設每兩秒執行：

```text
每隔一段時間取得 system usage
  -> 建立最新 metrics
  -> 重用既有 process adapter
  -> controller.getSnapshot(...)
  -> 更新 UI state
  -> View 重新繪製
```

State layer 會重用同一個 `ProcessManager`，讓 process CPU usage 保持連續。

## Layers in the package

### Model

`terrarium.model`：

- 定義水族缸的資料結構。
- 不讀取外部來源。
- 不操作 UI。
- 不執行 orchestration。

### Controller

`terrarium.controller`：

- 接收 adapters。
- 依序讀取來源。
- 隔離單一來源錯誤。
- 呼叫 Composer。
- 回傳最終 `TerrariumSnapshot`。

### Core

`terrarium.core`：

- 定義外部資源 adapter contract。
- 把外部資料翻譯成 terrarium signals。
- 合併 signals。
- 計算 environment 與 fish state。

### View

已實作於 Kotlin：

- `terrarium/Terrarium.kt`：組合 snapshot、場景與魚資訊 dialog。
- `terrarium/TerrariumScene.kt`：最多選取 100 隻魚、動畫、hover 與 click。
- `terrarium/Water.kt`：水色、濁度、洋流與氣泡。
- `terrarium/Environment.kt`：環境分段與 style contract。
- `terrarium/Substrate.kt`、`Rocks.kt`、`Plants.kt`、`Coral.kt`：各自繪製環境元件。
- `terrarium/Fish.kt`：依 kind、health、stress、status 與 visual hint 繪製魚。

每次 snapshot 更新時，環境數值、水色、環境配色、水草與珊瑚會用 2 秒
transition 漸變到新狀態。若 transition 尚未完成又收到新 snapshot，動畫會從
目前畫面中的中間值繼續追向新 target。

每隻魚第一次出現時會固定 position seed、軌跡與 facing direction。後續 snapshot
只替換它的狀態與 style，不會重新計算軌跡。新增魚會淡入；process/job 消失或
離開 top 100 時會先用 2 秒淡出，完成後才移除並讓下一隻魚補位。

魚的 seed 會決定跨越大部分水族缸寬高的路徑。每隻魚持有自己的
`horizontalProgress Animatable`，不使用循環 global phase 推導 X。抵達邊界後
會保持目前 X 700ms，再用 600ms 水平 scale transition 翻面；翻面完成後才從
同一個 X animate 到另一側，因此不會 wrap 或瞬移。

View 只讀 `TerrariumSnapshot`，不直接存取 jobs、system usage 或 processes。
