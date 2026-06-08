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

@Composable
fun App() {
    var selectedItem by remember { mutableIntStateOf(0) }
    var darkTheme by rememberSaveable { mutableStateOf(false) }

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
