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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import page.JobManagement
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.ui.draw.clip

@Composable
fun WindowScope.App(
    windowState: WindowState,
    onCloseRequest: () -> Unit
) {
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
                        launch(Dispatchers.IO) {
                            notifier.notify(
                                "系統 CPU 負載過高",
                                "CPU 使用率已連續 5 秒超過 90% (當前: ${cpu.roundToInt()}%)",
                                Notification.Status.WARNING
                            )
                        }
                    }
                } else {
                    consecutiveHighCpu = 0
                }

                // Memory Usage Check (> 90%)
                val mem = usage.memoryUsagePercent
                if (mem > 90.0) {
                    consecutiveHighMemory++
                    if (consecutiveHighMemory == 5) {
                        launch(Dispatchers.IO) {
                            notifier.notify(
                                "系統記憶體不足",
                                "記憶體使用率已連續 5 秒超過 90% (當前: ${mem.roundToInt()}%)",
                                Notification.Status.WARNING
                            )
                        }
                    }
                } else {
                    consecutiveHighMemory = 0
                }

                // CPU Temperature Check (> 80°C)
                val temp = usage.cpuTemperature
                if (temp > 80.0) {
                    val now = System.currentTimeMillis()
                    if (now - lastTempNotificationTime > cooldownMillis) {
                        launch(Dispatchers.IO) {
                            notifier.notify(
                                "CPU Overheating",
                                "CPU temperature reached ${temp.roundToInt()}°C! Please check cooling.",
                                Notification.Status.ERROR
                            )
                        }
                        lastTempNotificationTime = now
                    }
                }
            }
            delay(1000L)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var selectedItem by remember { mutableIntStateOf(0) }
    var darkTheme by rememberSaveable { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var historyLogs by remember { mutableStateOf(emptyList<SystemNotification.LogEntry>()) }



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
                        Button(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    SystemNotification.clearNotificationLogs()
                                }
                                historyLogs = emptyList()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("Clear All")
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
                                 val severityColor = if (darkTheme) {
                                     when (log.status) {
                                         "ERROR" -> Color(0xFFCF6679) // Red
                                         "WARNING" -> Color(0xFFFF9800) // Orange
                                         "INFO" -> Color(0xFFBB86FC) // Purple
                                         "SUCCESS" -> Color(0xFF81C784) // Green
                                         else -> Color(0xFFBB86FC)
                                     }
                                 } else {
                                     when (log.status) {
                                         "ERROR" -> Color(0xFFD32F2F) // Red
                                         "WARNING" -> Color(0xFFE65100) // Dark Orange
                                         "INFO" -> Color(0xFF7B1FA2) // Purple
                                         "SUCCESS" -> Color(0xFF388E3C) // Green
                                         else -> Color(0xFF7B1FA2)
                                     }
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
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CustomTitleBar(
                    windowState = windowState,
                    onCloseRequest = onCloseRequest,
                    darkTheme = darkTheme
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Scaffold(modifier = Modifier.weight(1f)) {
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
    }
}

@Composable
fun WindowScope.CustomTitleBar(
    windowState: WindowState,
    onCloseRequest: () -> Unit,
    darkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WindowDraggableArea(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CYBER",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (darkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "TERRARIUM",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val minInteraction = remember { MutableInteractionSource() }
            val minHovered by minInteraction.collectIsHoveredAsState()
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp)
                    .background(if (minHovered) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.Transparent)
                    .clickable(
                        interactionSource = minInteraction,
                        indication = null
                    ) {
                        windowState.isMinimized = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(10.dp, 1.dp).background(MaterialTheme.colorScheme.onSurface))
            }

            val maxInteraction = remember { MutableInteractionSource() }
            val maxHovered by maxInteraction.collectIsHoveredAsState()
            val isMaximized = windowState.placement == WindowPlacement.Maximized
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp)
                    .background(if (maxHovered) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.Transparent)
                    .clickable(
                        interactionSource = maxInteraction,
                        indication = null
                    ) {
                        windowState.placement = if (isMaximized) WindowPlacement.Floating else WindowPlacement.Maximized
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(9.dp)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface)
                )
            }

            val closeInteraction = remember { MutableInteractionSource() }
            val closeHovered by closeInteraction.collectIsHoveredAsState()
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp)
                    .background(if (closeHovered) Color(0xFFEF4444) else Color.Transparent)
                    .clickable(
                        interactionSource = closeInteraction,
                        indication = null
                    ) {
                        onCloseRequest()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = if (closeHovered) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
