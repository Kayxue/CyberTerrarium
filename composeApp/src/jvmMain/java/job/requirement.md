## Job Management System Features

- JobDefinition：Job 的主體規格。
  - 用途是保存使用者建立的任務內容（id、名稱、腳本、trigger、dependency、runtime、policy、enabled）。
  它不應該存「這次執行結果」，只存「任務定義」。

- JobRuntimeConfig：執行環境設定。
  - 用途是描述容器與語言環境，例如 runtimeType=PYTHON、image、cpu/mem limit、env、timeout、allowedPackages。
  這層讓你之後擴充 Java/C++/Lua 時，不用改 JobDefinition 結構。

- JobDependency：依賴邊（DAG edge）。
  - 用途是表示 jobA -> jobB 這種「B 需要 A 成功後才可跑」的關係。
  拆出來可以讓 DAG 驗證、拓樸排序更清楚，不會把圖演算法塞進 job 主類別。

- JobTrigger：觸發條件。
  - 用途是描述時間相關觸發（cron、fixed interval、manual）。
  依賴是「可不可以跑」，trigger 是「何時嘗試跑」，兩者分開很重要。

- JobExecutionPolicy：執行策略。
  - 用途是控制重試次數、backoff、並行限制、失敗是否阻斷下游等。
  這是排程策略，不是 runtime 參數，分開後比較好測試與調整。

- JobStatus：當前狀態（enum）。
  - 用途是統一狀態機，例如 QUEUED/RUNNING/SUCCESS/FAILED/TIMEOUT/CANCELLED。
  這能避免各模組自定義字串造成對接混亂。

- JobRunRecord：單次執行紀錄。
  - 用途是保存某次 run 的開始/結束時間、exitCode、stdout/stderr 摘要、失敗原因、使用資源。
  一個 JobDefinition 會對應很多筆 JobRunRecord（一對多）。

- RunContext：本次執行上下文。
  - 用途是把「本次」資料帶給 executor，例如工作目錄、暫存目錄、觸發來源、traceId。
  不要把這種短生命週期資料塞進 JobDefinition。

- RunResult：執行回傳結果。
  - 用途是 executor 統一輸出，讓 scheduler 不需要知道 Python 或 shell 細節。
  例如 status、exitCode、duration、errorType。