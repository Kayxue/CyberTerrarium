package com.kay.cyberterrarium.jobmanagement.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
    currentPageIsCatalog: Boolean,
    currentPageIsResults: Boolean,
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
    onOpenCatalog: () -> Unit,
    onOpenResults: () -> Unit,
    onOpenGraph: () -> Unit
) {
    var createExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .widthIn(min = 1280.dp)
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Job Management", style = MaterialTheme.typography.headlineSmall)

        SelectDropdownField(
            label = "Flow",
            value = selectedFlowId ?: "Select Flow",
            options = flowIds.ifEmpty { listOf("No Flows") },
            onSelect = { selected ->
                if (selected != "No Flows") {
                    onSelectFlow(selected)
                }
            },
            modifier = Modifier.width(230.dp)
        )

        OutlinedTextField(
            modifier = Modifier.width(118.dp),
            value = maxWorkersText,
            onValueChange = onMaxWorkersChange,
            singleLine = true,
            label = { Text("Workers") }
        )

        Spacer(modifier = Modifier.weight(1f))

        AppButton(
            onClick = { createExpanded = !createExpanded },
            variant = AppButtonVariant.MUTED,
            modifier = Modifier.widthIn(min = 44.dp, max = 52.dp)
        ) { Text("+") }

        if (createExpanded) {
            AppButton(onClick = onOpenCreateFlow, variant = AppButtonVariant.MUTED) { Text("Flow") }
            AppButton(onClick = onOpenCreateStage, variant = AppButtonVariant.MUTED) { Text("Stage") }
            AppButton(onClick = onOpenCreateJob, variant = AppButtonVariant.MUTED) { Text("Job") }
        }

        AppButton(
            onClick = onRunFlow,
            enabled = !selectedFlowId.isNullOrBlank(),
            variant = AppButtonVariant.SUCCESS
        ) { Text("Run") }

        AppButton(
            onClick = onOpenResults,
            variant = if (currentPageIsResults) AppButtonVariant.PRIMARY else AppButtonVariant.DEFAULT
        ) { Text("Results") }

        AppButton(
            onClick = onRefresh,
            variant = AppButtonVariant.DEFAULT
        ) { Text("Refresh") }

        AppButton(
            onClick = if (currentPageIsCatalog) onOpenGraph else onOpenCatalog,
            variant = if (currentPageIsCatalog) AppButtonVariant.PRIMARY else AppButtonVariant.DEFAULT
        ) { Text(if (currentPageIsCatalog) "Graph" else "Catalog") }

        if (!currentPageIsGraph) {
            AppButton(
                onClick = onOpenGraph,
                variant = AppButtonVariant.MUTED
            ) { Text("Back") }
        }

        AppButton(
            onClick = onClearFlow,
            variant = AppButtonVariant.DANGER
        ) { Text("Clear") }
    }
}
