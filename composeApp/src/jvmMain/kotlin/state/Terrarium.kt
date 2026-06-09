package state

import SystemUsageInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import db.DatabaseFactory
import job.controller.JobController
import job.repository.JobConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import process.ProcessManager
import terrarium.controller.TerrariumController
import terrarium.core.JobTerrariumAdapter
import terrarium.core.ProcessTerrariumAdapter
import terrarium.core.SystemUsageTerrariumAdapter
import terrarium.core.TerrariumResourceAdapter
import terrarium.model.TerrariumSnapshot
import terrarium.model.TerrariumSystemMetrics
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun rememberTerrariumSnapshot(
    systemUsage: SystemUsageInfo?,
    intervalMillis: Long = 2_000L
): TerrariumSnapshot {
    val latestSystemUsage = rememberUpdatedState(systemUsage)

    return produceState(
        initialValue = TerrariumSnapshot.empty(),
        key1 = intervalMillis
    ) {
        val controller = TerrariumController()
        val processAdapter = ProcessTerrariumAdapter(ProcessManager())
        val jobAdapter = runCatching {
            JobTerrariumAdapter(
                JobController.createDefault(),
                JobConfigRepository(DatabaseFactory.getInstance())
            )
        }.getOrNull()

        while (isActive) {
            val metrics = latestSystemUsage.value?.toTerrariumMetrics()
            value = withContext(Dispatchers.IO) {
                val adapters = buildList<TerrariumResourceAdapter> {
                    add(SystemUsageTerrariumAdapter(metrics))
                    if (jobAdapter != null) {
                        add(jobAdapter)
                    }
                    add(processAdapter)
                }
                controller.getSnapshot(adapters)
            }
            delay(intervalMillis.milliseconds)
        }
    }.value
}

private fun SystemUsageInfo.toTerrariumMetrics(): TerrariumSystemMetrics {
    return TerrariumSystemMetrics(
        cpuUsagePercent(),
        cpuCurrentFrequencyHz(),
        memoryUsedBytes(),
        memoryTotalBytes(),
        memoryUsagePercent(),
        downloadBytesPerSecond(),
        uploadBytesPerSecond(),
        cpuTemperature()
    )
}
