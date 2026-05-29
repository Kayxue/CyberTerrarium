package com.kay.cyberterrarium.jobmanagement.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
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
    val scrollState = rememberScrollState()
    val singleRowThreshold = 1180.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        val singleRow = maxWidth >= singleRowThreshold

        if (singleRow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LeftHeaderControls(
                    selectedFlowId = selectedFlowId,
                    flowIds = flowIds,
                    flowMenuExpanded = flowMenuExpanded,
                    onFlowMenuExpandedChange = { flowMenuExpanded = it },
                    onSelectFlow = onSelectFlow,
                    onClearFlow = onClearFlow,
                    maxWorkersText = maxWorkersText,
                    onMaxWorkersChange = onMaxWorkersChange
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RightHeaderControls(
                        onOpenCreateFlow = onOpenCreateFlow,
                        onOpenCreateJob = onOpenCreateJob,
                        onOpenCreateStage = onOpenCreateStage,
                        onRunFlow = onRunFlow,
                        onRefresh = onRefresh,
                        onTogglePage = onTogglePage,
                        currentPageIsGraph = currentPageIsGraph,
                        selectedFlowId = selectedFlowId
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LeftHeaderControls(
                    selectedFlowId = selectedFlowId,
                    flowIds = flowIds,
                    flowMenuExpanded = flowMenuExpanded,
                    onFlowMenuExpandedChange = { flowMenuExpanded = it },
                    onSelectFlow = onSelectFlow,
                    onClearFlow = onClearFlow,
                    maxWorkersText = maxWorkersText,
                    onMaxWorkersChange = onMaxWorkersChange
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Row(
                        modifier = Modifier.horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RightHeaderControls(
                            onOpenCreateFlow = onOpenCreateFlow,
                            onOpenCreateJob = onOpenCreateJob,
                            onOpenCreateStage = onOpenCreateStage,
                            onRunFlow = onRunFlow,
                            onRefresh = onRefresh,
                            onTogglePage = onTogglePage,
                            currentPageIsGraph = currentPageIsGraph,
                            selectedFlowId = selectedFlowId
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeftHeaderControls(
    selectedFlowId: String?,
    flowIds: List<String>,
    flowMenuExpanded: Boolean,
    onFlowMenuExpandedChange: (Boolean) -> Unit,
    onSelectFlow: (String) -> Unit,
    onClearFlow: () -> Unit,
    maxWorkersText: String,
    onMaxWorkersChange: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Job Management", style = MaterialTheme.typography.headlineSmall)
        AppButton(onClick = { onFlowMenuExpandedChange(true) }) {
            Text(if (selectedFlowId.isNullOrBlank()) "Select Flow" else selectedFlowId)
        }
        DropdownMenu(
            expanded = flowMenuExpanded,
            onDismissRequest = { onFlowMenuExpandedChange(false) }
        ) {
            flowIds.forEach { flowId ->
                DropdownMenuItem(
                    text = { Text(flowId) },
                    onClick = {
                        onFlowMenuExpandedChange(false)
                        onSelectFlow(flowId)
                    }
                )
            }
        }
        AppButton(onClick = onClearFlow) { Text("Clear") }
        OutlinedTextField(
            modifier = Modifier.width(128.dp),
            value = maxWorkersText,
            onValueChange = onMaxWorkersChange,
            singleLine = true,
            label = { Text("Workers") }
        )
    }
}

@Composable
private fun RightHeaderControls(
    onOpenCreateFlow: () -> Unit,
    onOpenCreateJob: () -> Unit,
    onOpenCreateStage: () -> Unit,
    onRunFlow: () -> Unit,
    onRefresh: () -> Unit,
    onTogglePage: () -> Unit,
    currentPageIsGraph: Boolean,
    selectedFlowId: String?
) {
    AppButton(onClick = onOpenCreateFlow) { Text("New Flow") }
    AppButton(onClick = onOpenCreateStage) { Text("New Stage") }
    AppButton(onClick = onOpenCreateJob) { Text("New Job") }
    AppButton(onClick = onRunFlow, enabled = !selectedFlowId.isNullOrBlank()) { Text("Run") }
    AppButton(onClick = onRefresh) { Text("Refresh") }
    AppButton(onClick = onTogglePage) {
        Text(if (currentPageIsGraph) "All Flows / Jobs" else "Back To Graph")
    }
}
