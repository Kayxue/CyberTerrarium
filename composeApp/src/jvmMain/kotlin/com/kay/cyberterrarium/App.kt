package com.kay.cyberterrarium

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kay.cyberterrarium.jobmanagement.JobManagement
import page.Home
import page.Processes
import page.Stats

@Composable
fun App() {
    var selectedItem by remember { mutableIntStateOf(0) }

    val items = listOf("Home", "Stats", "Process")

    val selectedIcons = listOf(
        Icons.Filled.Home,
        Icons.Filled.QueryStats,
        Icons.AutoMirrored.Filled.List
    )

    val unselectedIcons = listOf(
        Icons.Outlined.Home,
        Icons.Outlined.QueryStats,
        Icons.AutoMirrored.Outlined.List
    )

    MaterialTheme {
        Row(modifier = Modifier.fillMaxSize().width(32.dp)) {

            NavigationRail(modifier = Modifier.fillMaxHeight()) {
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
                        onClick = { selectedItem = index }
                    )
                }

                Spacer(Modifier.weight(1f))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            ) {
                when (selectedItem){
                    0 -> Home()
                    1 -> Stats()
                    2 -> Processes()
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
