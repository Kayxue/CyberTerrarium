package com.kay.cyberterrarium

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kay.cyberterrarium.jobmanagement.JobManagement

@Composable
fun App() {
    var selectedItem by remember { mutableIntStateOf(0) }

    val items = listOf("Terrarium", "Stats", "Management")

    val selectedIcons = listOf(
        Icons.Filled.Home,
        Icons.Filled.QueryStats,
        Icons.Filled.Settings
    )

    val unselectedIcons = listOf(
        Icons.Outlined.Home,
        Icons.Outlined.QueryStats,
        Icons.Outlined.Settings
    )

    MaterialTheme {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(modifier = Modifier.fillMaxHeight()) {
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
                        modifier = Modifier.padding(top = if(index == 0) 16.dp else 8.dp ),
                        onClick = { selectedItem = index }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (selectedItem) {
                    2 -> JobManagement()
                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = items[selectedItem],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = selectedIcons[selectedItem],
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
