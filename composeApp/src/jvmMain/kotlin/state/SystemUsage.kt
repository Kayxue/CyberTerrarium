package state

import SystemUsageInfo
import SystemUsageHistory
import SystemUsageSampler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun rememberSystemUsage(
    intervalMillis: Long = 1000L
): SystemUsageInfo? {
    return rememberSystemUsageState(intervalMillis).value
}

@Composable
fun rememberSystemUsageHistory(
    intervalMillis: Long = 1000L,
    maxPoints: Int = 60
): SystemUsageHistory {
    return produceState(
        initialValue = SystemUsageHistory.empty(),
        key1 = intervalMillis,
        key2 = maxPoints
    ) {
        val sampler = SystemUsageSampler(maxPoints)
        while (isActive) {
            value = withContext(Dispatchers.IO) {
                sampler.sampleHistory()
            }
            delay(intervalMillis.milliseconds)
        }
    }.value
}

@Composable
private fun rememberSystemUsageState(
    intervalMillis: Long
): State<SystemUsageInfo?> {
    return produceState<SystemUsageInfo?>(initialValue = null, key1 = intervalMillis) {
        val sampler = SystemUsageSampler(1)
        while (isActive) {
            value = withContext(Dispatchers.IO) {
                sampler.sampleLatest()
            }
            delay(intervalMillis.milliseconds)
        }
    }
}
