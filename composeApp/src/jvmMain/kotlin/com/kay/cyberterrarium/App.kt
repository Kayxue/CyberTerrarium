package com.kay.cyberterrarium

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Light
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kay.cyberterrarium.jobmanagement.JobManagement
import com.kay.cyberterrarium.theme.AppTheme
import page.Home
import page.Processes
import page.Stats
import SystemUsageSampler
import notification.service.SystemNotification
import notification.model.Notification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun App() {
    LaunchedEffect(Unit) {
        val sampler = SystemUsageSampler(1)
        val notifier = SystemNotification.getInstance()

        var consecutiveHighCpu = 0
        var consecutiveHighMemory = 0
        var lastTempNotificationTime = 0L
        val cooldownMillis = 60_000L // 1 minute cooldown

        while (isActive) {
            val usage = withContext(Dispatchers.IO) {
                sampler.sampleLatest()
            }

            if (usage != null) {
                // CPU Usage Check (> 90%)
                val cpu = usage.cpuUsagePercent
                if (cpu > 90.0) {
                    consecutiveHighCpu++
                    if (consecutiveHighCpu == 5) {
                        notifier.notify(
                            "系統 CPU 負載過高",
                            "CPU 使用率已連續 5 秒超過 90% (當前: ${cpu.roundToInt()}%)",
                            Notification.Status.WARNING
                        )
                    }
                } else {
                    consecutiveHighCpu = 0
                }

                // Memory Usage Check (> 90%)
                val mem = usage.memoryUsagePercent
                if (mem > 90.0) {
                    consecutiveHighMemory++
                    if (consecutiveHighMemory == 5) {
                        notifier.notify(
                            "系統記憶體不足",
                            "記憶體使用率已連續 5 秒超過 90% (當前: ${mem.roundToInt()}%)",
                            Notification.Status.WARNING
                        )
                    }
                } else {
                    consecutiveHighMemory = 0
                }

                // CPU Temperature Check (> 80°C)
                val temp = usage.cpuTemperature
                if (temp > 80.0) {
                    val now = System.currentTimeMillis()
                    if (now - lastTempNotificationTime > cooldownMillis) {
                        notifier.notify(
                            "CPU Overheating",
                            "CPU temperature reached ${temp.roundToInt()}°C! Please check cooling.",
                            Notification.Status.ERROR
                        )
                        lastTempNotificationTime = now
                    }
                }
            }
            delay(1000L)
        }
    }

    var selectedItem by remember { mutableIntStateOf(0) }
    var darkTheme by rememberSaveable { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var historyLogs by remember { mutableStateOf(emptyList<SystemNotification.LogEntry>()) }

    if (showHistoryDialog) {
        LaunchedEffect(showHistoryDialog) {
            historyLogs = withContext(Dispatchers.IO) {
                SystemNotification.getNotificationLogs()
            }
        }

        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notification History")
                    TextButton(
                        onClick = {
                            SystemNotification.clearNotificationLogs()
                            historyLogs = emptyList()
                        }
                    ) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            text = {
                if (historyLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No notifications", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyLogs.size) { index ->
                            val log = historyLogs[index]
                            val severityColor = when (log.status) {
                                "ERROR" -> MaterialTheme.colorScheme.error
                                "WARNING" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .padding(top = 4.dp)
                                            .background(
                                                color = severityColor,
                                                shape = CircleShape
                                            )
                                    )
                                    Column {
                                        Text(
                                            text = log.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = log.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = log.createdAt,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showHistoryDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    val items = listOf("Home", "Stats", "Process", "Jobs")

    val selectedIcons = listOf(
        Icons.Filled.Home,
        Icons.Filled.QueryStats,
        Icons.AutoMirrored.Filled.List,
        Icons.Filled.Settings
    )

    val unselectedIcons = listOf(
        Icons.Outlined.Home,
        Icons.Outlined.QueryStats,
        Icons.AutoMirrored.Outlined.List,
        Icons.Outlined.Settings
    )

    AppTheme(darkTheme = darkTheme) {
        Scaffold {
            Row(modifier = Modifier.fillMaxSize().width(32.dp)) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(Modifier.weight(1f))

                    items.forEachIndexed { index, item ->
                        NavigationRailItem(
                            icon = {
                                Icon(
                                    imageVector = if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                                    contentDescription = item,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(item) },
                            selected = selectedItem == index,
                            modifier = Modifier.padding(top = if (index == 0) 16.dp else 8.dp),
                            onClick = { selectedItem = index },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Box(modifier = Modifier.padding(bottom = 8.dp)){
                        FloatingActionButton(onClick = { showHistoryDialog = true }){
                            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications Icon")
                        }
                    }

                    Box(modifier = Modifier.padding(bottom = 16.dp)){
                        FloatingActionButton(onClick = {darkTheme = !darkTheme}){
                            Icon(Icons.Outlined.Light, contentDescription = "Dark Icon")
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                ) {
                    when (selectedItem) {
                        0 -> Home()
                        1 -> Stats()
                        2 -> Processes()
                        3 -> JobManagement()
                    }
                }
            }
        }
    }
}
