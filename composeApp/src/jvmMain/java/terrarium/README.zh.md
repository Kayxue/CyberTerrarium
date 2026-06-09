# Terrarium MVC 整合說明

這個 package 負責 terrarium 功能的 Model 與 Controller。Compose View 位於
`src/jvmMain/kotlin/terrarium`，頂層 `Terrarium` component 由 `page/Home.kt`
嵌入。View 只消費 `TerrariumSnapshot`。

## Package 分層

- `terrarium.model`
  - 水族缸 domain 的資料 contract。
  - 重要類別：
    - `TerrariumSnapshot`：提供給 View 的最終狀態。
    - `TerrariumEnvironmentState`：合成後的水族缸環境狀態。
    - `TerrariumFishState`：合成後的魚狀態。
    - `TerrariumSourceSnapshot`：單一外部資源正規化後的輸出。
    - `TerrariumEnvironmentSignal`：單一來源對環境造成的影響。
    - `TerrariumCreatureSignal`：單一來源提供的生物/魚訊號。
- `terrarium.core`
  - 純邏輯轉換與合成。
  - 重要類別：
    - `TerrariumResourceAdapter`：外部資源接入 terrarium 的 read-only contract。
    - `JobTerrariumAdapter`：把現有 jobs 轉成魚訊號。
    - `SystemUsageTerrariumAdapter`：把 system metrics 轉成環境訊號。
    - `ProcessTerrariumAdapter`：把完整 process tree 轉成魚訊號。
    - `TerrariumSnapshotComposer`：把多個 source snapshot 合成一個 terrarium snapshot。
- `terrarium.controller`
  - MVC 的 Controller 層。
  - 重要類別：
    - `ITerrariumController`
    - `TerrariumController`

Terrarium 沒有自己的 repository layer。Job adapter 可注入既有
`IJobConfigRepository` 讀取 priority，但絕不呼叫寫入方法。

## 主要 Contract

外部資源要進入 terrarium，應該透過 `TerrariumResourceAdapter`。

```java
public interface TerrariumResourceAdapter {
    String getSourceId();

    String getDisplayName();

    TerrariumSourceSnapshot readSnapshot();
}
```

規則：

- `readSnapshot()` 必須是 read-only。
- adapter 不可以 create、update、delete、run 或 mutate 外部資源。
- adapter 回傳前，要先把外部概念轉成 terrarium 的語意值。
- 如果來源不可用，回傳 `TerrariumSourceSnapshot.unavailable(...)`。
- 預期中的不可用狀態不要用 exception 表達。exception 保留給非預期錯誤；`TerrariumController` 會捕捉錯誤並轉成 unavailable source snapshot。

## 資料流

```text
外部資源
  -> TerrariumResourceAdapter.readSnapshot()
  -> TerrariumSourceSnapshot
  -> TerrariumController.getSnapshot(...)
  -> TerrariumSnapshotComposer
  -> TerrariumSnapshot
  -> Compose View layer
```

View 只會 render：

- `snapshot.getEnvironment()`
- `snapshot.getFish()`
- 必要時可用 `snapshot.getSources()` 顯示來源狀態或 debug 資訊

View 不應該直接呼叫 `JobController`、`SystemUsageSampler`、OSHI 或 process reader。

## 接入 Jobs

Jobs 已經可以透過 `JobTerrariumAdapter` 接入。

```java
IJobController jobController = JobController.createDefault();

TerrariumController terrariumController = new TerrariumController();
TerrariumSnapshot snapshot = terrariumController.getSnapshot(List.of(
    new JobTerrariumAdapter(jobController)
));
```

`JobTerrariumAdapter` 目前只讀：

- `IJobController.listJobs()`
- `IJobController.listFlowRuns()`
- `IJobController.listFlowRunJobs(...)`
- 可選擇使用 `IJobConfigRepository.findOneById(...)` 取得顯示 priority

它不會呼叫會修改 jobs 的 API。

轉換規則：

- 每個 job 會變成一個 `TerrariumCreatureSignal`。
- 最新結果成功：魚比較健康、比較活躍。
- 最新結果 failed / cancelled / timeout：魚會偏生病，risk/stress 較高。
- disabled job：魚會是 inactive。
- 還沒有 result：魚狀態 unknown，但健康值中等。
- Job priority：轉成 `visualHint.importance`，供 100 隻上限排序。

之後 `TerrariumSnapshotComposer` 會再把環境壓力套用到最終的 `TerrariumFishState`。

## 接入 System Usage

System usage 已經可以透過 `SystemUsageTerrariumAdapter` 接入，但這個 adapter 不直接依賴目前的 `SystemUsageInfo` 類別。
這是刻意的：`SystemUsageInfo` 在 Java default package，而具名 Java package 不能安全地 import default-package 類別。

請用 `TerrariumSystemMetrics` 作為橋接值：

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

未來從 Kotlin/Compose 接入時，大致會像這樣：

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

轉換規則：

- CPU 高會增加 stress 並降低環境 health。
- memory usage 高會降低 clarity 與 health。
- CPU temperature 高會增加 temperature stress 與 toxicity。
- network throughput 高會增加 water motion。
- system usage 不產生魚，只產生 environment signals。

## 接入 Processes

Processes 已透過 `ProcessTerrariumAdapter` 接入。它只呼叫 read-only 的
`ProcessManager.getProcessTrees()`。

```java
ProcessManager processManager = new ProcessManager();

TerrariumSnapshot snapshot = new TerrariumController().getSnapshot(List.of(
    new ProcessTerrariumAdapter(processManager)
));
```

請跨 snapshots 重用同一個 adapter 或 `ProcessManager`，因為它保存前一次
OSHI process ticks，後續 CPU usage 才會較準確。

所有 root 與 descendant process 都會成為魚。PID 控制 identity/位置，
name/user 控制顏色，resident memory 控制大小，CPU/state 控制活動、壓力、
風險、健康與移動。Process adapter 不產生 environment signals。

## 合併多個來源

正常使用時，會把所有可用來源一起交給 controller：

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

`TerrariumController` 會隔離 adapter failure。若其中一個 adapter 失敗，controller 會為那個 adapter 建立 unavailable source snapshot，並且仍然回傳可 render 的 `TerrariumSnapshot`。

## View 整合

View layer 不重複 mapping 規則，只 render model：

- 使用 `TerrariumEnvironmentState.health`、`clarity`、`stress`、`temperatureStress`、`waveIntensity`、`bubbleIntensity`、`tint` 來決定水的視覺。
- 使用每個 `TerrariumFishState.health`、`stress`、`activity`、`risk`、`status`、`visualHint` 來決定魚的視覺。
- 必要時使用 `TerrariumSourceSnapshot.sourceStatus` 與 `message` 顯示來源可用狀態。
- `state.rememberTerrariumSnapshot(...)` 預設每兩秒更新並重用 `ProcessManager`。
- 最多繪製 100 隻魚，依 `importance`、job 類型、risk、activity 排序。
- 環境數值、水色、環境配色、水草與珊瑚會在 2 秒內漸變到新 snapshot，
  不會瞬間跳換。
- Snapshot 更新時，既有魚會維持原軌跡與方向，只更新狀態和 style。新增魚會
  淡入，消失或被替換的魚會在 2 秒內淡出。
- 每隻魚都有獨立的水平位置動畫，不再用循環 global phase 推導 X。抵達左右
  邊界後會保持原位 700ms，再用 600ms 左右翻面；翻面完成後才從完全相同的
  位置往反方向移動，不會 wrap 或瞬移。
- Hover 顯示精簡資訊，點擊魚會開啟詳細 dialog。

如果 View 需要新的視覺提示，優先檢查現有語意值是否足夠。只有當現有值不足以表達需求時，才在 model/core 增加新的語意欄位。
避免讓 View 直接檢查 job/process/system 的外部類別。

## 目前限制

- Terrarium 沒有 DB repository，因為目前不持久化資料。
- Jobs 只透過現有 job controller 的 read/list API 讀取。
- System usage 必須透過 `TerrariumSystemMetrics` 橋接。
- Processes 會攤平，暫不視覺化 parent/child 關係。
- 超過 100 隻時，其餘魚仍保留在 snapshot。
