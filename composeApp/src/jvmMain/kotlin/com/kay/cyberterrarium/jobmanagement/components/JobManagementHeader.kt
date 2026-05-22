package com.kay.cyberterrarium.jobmanagement.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun JobManagementHeader(
    currentPageIsGraph: Boolean,
    selectedFlowId: String?,
    flowIds: List<String>,
    maxWorkersText: String,
    onMaxWorkersChange: (String) -> Unit,
    onSelectFlow: (String) -> Unit,
    onClearFlow: () -> Unit,
    onOpenCreateFlow: () -> Unit,
    onOpenCreateJob: () -> Unit,
    onOpenCreateStage: () -> Unit,
    onRunFlow: () -> Unit,
    onRefresh: () -> Unit,
    onTogglePage: () -> Unit
) {
    var flowMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Job Management", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { flowMenuExpanded = true }) {
                Text(if (selectedFlowId.isNullOrBlank()) "Select Flow" else selectedFlowId)
            }
            DropdownMenu(
                expanded = flowMenuExpanded,
                onDismissRequest = { flowMenuExpanded = false }
            ) {
                flowIds.forEach { flowId ->
                    DropdownMenuItem(
                        text = { Text(flowId) },
                        onClick = {
                            flowMenuExpanded = false
                            onSelectFlow(flowId)
                        }
                    )
                }
            }
            Button(onClick = onClearFlow) { Text("Clear") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = maxWorkersText,
                onValueChange = onMaxWorkersChange,
                singleLine = true,
                label = { Text("Workers") }
            )
            Button(onClick = onOpenCreateFlow) { Text("New Flow") }
            Button(onClick = onOpenCreateStage) { Text("New Stage") }
            Button(onClick = onOpenCreateJob) { Text("New Job") }
            Button(onClick = onRunFlow, enabled = !selectedFlowId.isNullOrBlank()) { Text("Run") }
            Button(onClick = onRefresh) { Text("Refresh") }
            Button(onClick = onTogglePage) {
                Text(if (currentPageIsGraph) "All Flows / Jobs" else "Back To Graph")
            }
        }
    }
}
