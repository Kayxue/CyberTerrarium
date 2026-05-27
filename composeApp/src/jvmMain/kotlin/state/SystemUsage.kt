package state

import SystemUsageInfo
import SystemUsageMonitor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun rememberSystemUsage(
    intervalMillis: Long = 1000L
): SystemUsageInfo? {
    val monitor = remember { SystemUsageMonitor() }

    var usage by remember {
        mutableStateOf<SystemUsageInfo?>(null)
    }

    LaunchedEffect(Unit) {
        while (true) {
            usage = monitor.usage
            delay(intervalMillis.milliseconds)
        }
    }

    return usage
}