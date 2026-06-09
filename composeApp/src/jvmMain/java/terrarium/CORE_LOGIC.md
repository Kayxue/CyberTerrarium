# Terrarium 核心運作流程

Terrarium 的核心流程可以簡化成：

```text
外部原始資料
  -> Adapter 翻譯成水族缸語意
  -> Controller 收集各來源結果
  -> Composer 合併與計算
  -> TerrariumSnapshot
  -> 未來由 View 繪製水與魚
```

目前這個 package 實作的是 MVC 架構中的 Model 與 Controller。View 尚未接入。

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
- Processes 尚未支援。

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
    new UnavailableProcessTerrariumAdapter()
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

Terrarium 不直接操作 job DB，也不修改 job controller、repository 或 core。

每個 job 會產生一個 `TerrariumCreatureSignal`：

- `SUCCESS`：健康度與活動度較高。
- `FAILED`：生病、壓力與風險較高。
- `TIMEOUT`：健康度最低，風險最高。
- `RUNNING`：活動度高，壓力偏高。
- Disabled job：狀態為 `INACTIVE`。
- 沒有執行結果：狀態為 `UNKNOWN`，健康值中等。

目前 job 執行架構主要在 job 結束後保存結果，因此 terrarium 現階段主要呈現
最近一次執行結果。即時 `RUNNING` 狀態必須等外部 job 模組提供可讀取的即時狀態
後才能完整呈現。

### 5. Process 暫時回傳 Unavailable

目前專案還沒有 process reader，因此使用：

```java
new UnavailableProcessTerrariumAdapter();
```

它只會回傳：

```text
sourceStatus = UNAVAILABLE
environmentSignals = []
creatureSignals = []
message = process reader 尚未實作
```

它不會建立假的 process 魚。

未來 process reader 完成後，只需要新增真正的 `ProcessTerrariumAdapter`，把每個
process 轉成 `TerrariumCreatureSignal`，再替換掉 unavailable adapter。

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

未來 View 只需要讀取：

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
    new UnavailableProcessTerrariumAdapter()
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

## 目前不會自動更新

目前 Controller 的行為是：

```text
呼叫一次 getSnapshot()
  -> 讀取一次所有 adapters
  -> 產生一次 TerrariumSnapshot
```

`TerrariumSnapshot` 本身不會自動變化。

未來 View 或 state layer 需要定時執行：

```text
每隔一段時間取得 system usage
  -> 建立最新 metrics/adapters
  -> controller.getSnapshot(...)
  -> 更新 UI state
  -> View 重新繪製
```

例如每秒產生一個新的 snapshot，就能讓水族缸持續反映最新系統狀態。

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

尚未實作。未來 View 應該：

- 只讀 `TerrariumSnapshot`。
- 根據 `TerrariumEnvironmentState` 繪製水色、濁度、波浪與氣泡。
- 根據 `TerrariumFishState` 繪製魚的健康、活動、壓力與風險。
- 不直接存取 jobs、system usage 或 processes。
